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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.lable.oss.dynamicconfig.provider.file.FileWatcher.Event;
import org.lable.oss.dynamicconfig.test.FileUtil;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class FileWatcherIT {
    private static Path dir;

    @BeforeClass
    public static void setup() throws IOException {
        dir = Files.createTempDirectory("filewatcherit");
    }


    @Test
    public void test() throws IOException, InterruptedException {
        /*
            Setup the watcher.
         */

        final List<PathEvent> pathEvents = new ArrayList<>();

        // The latch is set to the number of expected events.
        final CountDownLatch latch = new CountDownLatch(7);

        FileWatcher fileWatcher = new FileWatcher(
                (event, filePath) -> {
                    pathEvents.add(new PathEvent(event, filePath));
                    latch.countDown();
                },
                dir
        );

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(fileWatcher);

        /*
            Execute the test.
         */

        Files.createFile(dir.resolve("ignored"));
        Files.createFile(dir.resolve("temp.txt"));
        Files.createFile(dir.resolve("temp2.txt"));

        TimeUnit.SECONDS.sleep(1);

        assertThat(pathEvents, is(empty()));

        fileWatcher.listen(Paths.get("temp.txt"));
        fileWatcher.listen(Paths.get("temp2.txt"));

        TimeUnit.SECONDS.sleep(1);

        modify(dir.resolve("temp.txt"));

        TimeUnit.MILLISECONDS.sleep(100);

        modify(dir.resolve("temp.txt"));

        TimeUnit.MILLISECONDS.sleep(100);

        Files.deleteIfExists(dir.resolve("temp.txt"));

        TimeUnit.MILLISECONDS.sleep(100);

        Files.createFile(dir.resolve("temp.txt"));

        TimeUnit.MILLISECONDS.sleep(100);

        modify(dir.resolve("temp.txt"));

        TimeUnit.SECONDS.sleep(1);

        fileWatcher.stopListening(Paths.get("temp.txt"));

        TimeUnit.SECONDS.sleep(1);

        modify(dir.resolve("temp.txt"));
        modify(dir.resolve("temp2.txt"));
        Files.deleteIfExists(dir.resolve("temp2.txt"));

        // Wait for all expected events to be received.
        if (!latch.await(5L, TimeUnit.SECONDS)) {
            fail();
        }

        /*
            Verify.
         */

        assertThat(pathEvents.get(0), is(new PathEvent(Event.FILE_MODIFIED, Paths.get("temp.txt"))));
        assertThat(pathEvents.get(1), is(new PathEvent(Event.FILE_MODIFIED, Paths.get("temp.txt"))));
        assertThat(pathEvents.get(2), is(new PathEvent(Event.FILE_DELETED, Paths.get("temp.txt"))));
        assertThat(pathEvents.get(3), is(new PathEvent(Event.FILE_CREATED, Paths.get("temp.txt"))));
        assertThat(pathEvents.get(4), is(new PathEvent(Event.FILE_MODIFIED, Paths.get("temp.txt"))));
        assertThat(pathEvents.get(5), is(new PathEvent(Event.FILE_MODIFIED, Paths.get("temp2.txt"))));
        assertThat(pathEvents.get(6), is(new PathEvent(Event.FILE_DELETED, Paths.get("temp2.txt"))));

        /*
            Clean up.
         */

        fileWatcher.close();
        executorService.shutdown();

        if (!executorService.awaitTermination(5L, TimeUnit.SECONDS)) {
            fail();
        }
    }

    // Arbitrarily modify a file; just to trigger a modification event.
    void modify(Path file) throws IOException {
        Files.write(file, new byte[]{1});
    }

    @AfterClass
    public static void teardown() throws IOException {
        // Remove the testing directory.
        FileUtil.deleteDirAndContents(dir);
    }

    static class PathEvent {
        Event event;
        Path path;

        public PathEvent(Event event, Path path) {
            this.event = event;
            this.path = path;
        }

        @Override
        public int hashCode() {
            return Objects.hash(event, path);
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof PathEvent)) return false;
            PathEvent that = (PathEvent) other;

            return Objects.equals(event, that.event) && Objects.equals(path, that.path);
        }

        @Override
        public String toString() {
            return event + " > " + path;
        }
    }
}