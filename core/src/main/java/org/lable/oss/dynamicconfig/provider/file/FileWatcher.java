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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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

    final WatchService watchService;
    final Callback callback;
    final Path dirPath;
    final Set<Path> monitoredFiles;
    final Map<WatchKey, DirectoryContext> watchedDirectories;
    State state = State.RUNNING;

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
        this.watchedDirectories = new HashMap<>();

        watchService = dirPath.getFileSystem().newWatchService();
    }

    public synchronized void listen(Path filePath) {
        WatchKey watchKey;
        Path parentDir = dirPath.resolve(filePath).getParent();
        boolean alreadyWatched = false;
        for (DirectoryContext directoryContext : watchedDirectories.values()) {
            if (directoryContext.getPath().equals(parentDir)) {
                // Directory already watched.
                directoryContext.increment();
                alreadyWatched = true;
                break;
            }
        }

        if (!alreadyWatched) {
            try {
                watchKey = parentDir.register(watchService, ENTRY_MODIFY, ENTRY_CREATE, ENTRY_DELETE);
                // NoSuchFileException
            } catch (IOException e) {
                logger.error("Failed to register watcher at path " + parentDir, e);
                return;
            }
            this.watchedDirectories.put(watchKey, new DirectoryContext(parentDir));
            logger.info("Started listening to directory {} for changes.", parentDir);
        }

        this.monitoredFiles.add(filePath);
    }

    public synchronized void stopListening(Path filePath) {
        if (this.monitoredFiles.remove(filePath)) {
            Path fileParent = filePath.getParent();
            Path parentDir = fileParent == null ? dirPath : dirPath.resolve(fileParent);
            watchedDirectories.entrySet().removeIf(entry -> {
                DirectoryContext directoryContext = entry.getValue();
                if (directoryContext.getPath().equals(parentDir)) {
                    directoryContext.decrement();
                }
                return directoryContext.noFilesWatched();
            });
        }
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

                for (WatchEvent<?> event : key.pollEvents()) {
                    // Ignore all events that are not 'Path' (file/dir) modifications.
                    if (event.kind().type() != Path.class) continue;

                    // Now it is safe to cast to WatchEvent<Path> instead of WatchEvent<?>.
                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;

                    Path eventPath = pathEvent.context();
                    Path eventDir = watchedDirectories.get(key).getPath();
                    Path relativePath = dirPath.relativize(eventDir).resolve(eventPath);

                    if (eventPath.toFile().isDirectory()) {
                        System.out.println("!!!!!!");
                    } else {
                        // Ignore files we're not interested in.
                        if (!monitoredFiles.contains(relativePath)) continue;

                        Event eventType = Event.eventFromWatchEventKind(pathEvent.kind());
                        if (eventType != null) {
                            callback.fileChanged(eventType, relativePath);
                        }
                    }
                }

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

    static class DirectoryContext {
        int watchedFiles = 1;
        Path dirPath;

        DirectoryContext(Path dirPath) {
            this.dirPath = dirPath;
        }

        Path getPath() {
            return dirPath;
        }

        void increment() {
            watchedFiles++;
        }

        void decrement() {
            watchedFiles--;
        }

        boolean noFilesWatched() {
            return watchedFiles == 0;
        }
    }
}
