/*
 * Copyright (C) 2015 Lable (info@lable.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lable.oss.dynamicconfig.provider.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * A file monitoring runnable that watches a single file for modifications using {@link WatchService}.
 * <p>
 * Normally, {@link WatchService} watches all files in a directory for a number of event types. This class provides a
 * way to monitor only the file created, deleted, and modified events for a single specific file.
 */
public class FileWatcher implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(FileWatcher.class);

    private final WatchService watchService;
    private final Path filePath;
    private final Callback callback;

    /**
     * Construct a new FileWatcher.
     *
     * @param callback Callback to call when the file is modified.
     * @param filePath Path to the file to watch.
     * @throws IOException Thrown when the {@link WatchService} cannot be acquired.
     */
    public FileWatcher(Callback callback, Path filePath) throws IOException {
        this.filePath = filePath;
        this.callback = callback;

        Path dirContainingFile = filePath.getParent();
        watchService = dirContainingFile.getFileSystem().newWatchService();
        dirContainingFile.register(watchService, ENTRY_MODIFY, ENTRY_CREATE, ENTRY_DELETE);
    }

    @Override
    public void run() {
        logger.info("Starting file watcher for " + filePath);
        try {
            WatchKey key = watchService.take();
            // Poll the watch service for file modification events.
            while (key != null) {
                for (WatchEvent event : key.pollEvents())
                    if (isThisOurTargetFile(event, filePath)) {
                        Event eventType = Event.eventFromWatchEventKind(event.kind());
                        if (eventType != null) {
                            callback.fileChanged(eventType, filePath);
                        }
                    }
                key.reset();
                key = watchService.take();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Java's {@link WatchService} can't watch a single file, only directories and all files in it.
     * This method checks if the event returned belongs to the file we are interested in.
     *
     * @param event File watcher event.
     * @return True if the event was fired for the file we are watching.
     */
    static boolean isThisOurTargetFile(WatchEvent event, Path configPath) {
        // All contexts returned are Path instances for create, delete, and modify.
        Object context = event.context();
        if (context != null && context instanceof Path) {
            Path relativePath = (Path) context;
            Path dirContainingConfig = configPath.getParent();
            Path fullPath = dirContainingConfig.resolve(relativePath);
            // We are only interested in changes to the configuration file,
            // not in any other files in the watched directory.
            if (fullPath.equals(configPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Implement this callback interface to act on file modification events.
     */
    public interface Callback {
        /**
         * Called when the watched file is created, deleted, or modified.
         *
         * @param event    Event type.
         * @param filePath Path of the modified file.
         */
        void fileChanged(Event event, Path filePath);
    }

    /**
     * File modification events.
     * <p>
     * These map to the file created, deleted, and modified events in {@link WatchEvent.Kind}.
     */
    public enum Event {
        FILE_MODIFIED,
        FILE_CREATED,
        FILE_DELETED;

        /**
         * Maps this enum to instances of {@link WatchEvent.Kind}.
         *
         * @param kind Event kind returned in the watch event.
         * @return The matching enum value, or null if kind is of an unfamiliar type.
         */
        public static Event eventFromWatchEventKind(WatchEvent.Kind kind) {
            if (kind == null) {
                return null;
            }

            switch (kind.name()) {
                case "ENTRY_CREATE":
                    return FILE_CREATED;
                case "ENTRY_DELETE":
                    return FILE_DELETED;
                case "ENTRY_MODIFY":
                    return FILE_MODIFIED;
                default:
                    return null;
            }
        }
    }
}
