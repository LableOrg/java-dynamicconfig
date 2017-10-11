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
import java.nio.file.*;
import java.util.HashSet;
import java.util.Set;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * A file monitoring runnable that watches selected files for modifications using {@link WatchService}.
 * <p>
 * Normally, {@link WatchService} watches all files in a directory for a number of event types. This class provides a
 * way to monitor only the file created, deleted, and modified events for a specific set of files.
 */
public class FileWatcher implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(FileWatcher.class);

    private final WatchService watchService;
    private final Callback callback;
    private final Path dirPath;
    private final Set<Path> monitoredFiles;
    private State state = State.RUNNING;

    /**
     * Construct a new FileWatcher.
     *
     * @param dirPath  Path to the base directory to watch.
     * @throws IOException Thrown when the {@link WatchService} cannot be acquired.
     */
    public FileWatcher(Callback callback, Path dirPath) throws IOException {
        this.callback = callback;
        this.dirPath = dirPath;
        this.monitoredFiles = new HashSet<>();

        watchService = dirPath.getFileSystem().newWatchService();
        dirPath.register(watchService, ENTRY_MODIFY, ENTRY_CREATE, ENTRY_DELETE);
    }

    public synchronized void listen(Path filePath) {
        logger.info("Started listening to file for {} changes.", filePath);
        this.monitoredFiles.add(filePath);
    }

    public synchronized void stopListening(Path filePath) {
        this.monitoredFiles.remove(filePath);
    }

    @Override
    public void run() {
        logger.info("Starting file watcher for {}.", dirPath);
        try {
            WatchKey key = watchService.take();
            // Poll the watch service for file modification events.
            while (state == State.RUNNING && key != null) {
                // (Usually) prevent receiving two separate ENTRY_MODIFY events: file modified
                // and file access time updated. Instead, receive one ENTRY_MODIFY event with two counts.
                //
                // Cf.: https://stackoverflow.com/a/25221600/1641860
                Thread.sleep(50);

                key.pollEvents().stream()
                        // Ignore all events that are not 'Path' (file/dir) modifications.
                        .filter(event -> event.kind().type() == Path.class)
                        // Now it is safe to cast to WatchEvent<Path> instead of WatchEvent<?>.
                        .map(event -> {
                            @SuppressWarnings("unchecked")
                            WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                            return pathEvent;
                        })
                        // Ignore files we're not interested in.
                        .filter(pathEvent -> isThisOneOfOurTargetFiles(pathEvent, monitoredFiles))
                        .forEach(pathEvent -> {
                            Event eventType = Event.eventFromWatchEventKind(pathEvent.kind());
                            if (eventType != null) {
                                callback.fileChanged(eventType, pathEvent.context());
                            }
                        });
                key.reset();
                key = watchService.take();
            }
        } catch (ClosedWatchServiceException e) {
            // Shutting down the watcher. We're done.
            logger.info("Stopping file watcher for {}.", dirPath);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void close() throws IOException {
        state = State.SHUTTING_DOWN;
        watchService.close();
    }

    /**
     * Java's {@link WatchService} can't watch a single file, only directories and all files in it.
     * This method checks if the event returned belongs to the file we are interested in.
     *
     * @param event          File watcher event.
     * @param monitoredFiles Files we are interested in (relative to the directory monitored).
     * @return True if the event was fired for the file we are watching.
     */
    static boolean isThisOneOfOurTargetFiles(WatchEvent<Path> event, Set<Path> monitoredFiles) {
        Path relativePath = event.context();
        return monitoredFiles.contains(relativePath);
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

    enum State {
        RUNNING,
        SHUTTING_DOWN
    }
}
