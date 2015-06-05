/**
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

import org.junit.Test;

import java.nio.file.WatchEvent;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FileWatcherTest {
    @Test
    public void testEventFromWatchEventKindNull() {
        FileWatcher.Event result = FileWatcher.Event.eventFromWatchEventKind(null);
        assertThat(result, is(nullValue()));
    }

    @Test
    public void testEventFromWatchEventKindBogusInput() {
        WatchEvent.Kind mockKind = mock(WatchEvent.Kind.class);
        when(mockKind.name()).thenReturn("BOGUS");

        FileWatcher.Event result = FileWatcher.Event.eventFromWatchEventKind(mockKind);
        assertThat(result, is(nullValue()));
    }

    @Test
    public void testEventFromWatchEventKindValidInput() {
        WatchEvent.Kind mockKind = mock(WatchEvent.Kind.class);
        when(mockKind.name())
                .thenReturn("ENTRY_CREATE")
                .thenReturn("ENTRY_DELETE")
                .thenReturn("ENTRY_MODIFY");

        FileWatcher.Event result;

        result = FileWatcher.Event.eventFromWatchEventKind(mockKind);
        assertThat(result, is(FileWatcher.Event.FILE_CREATED));
        result = FileWatcher.Event.eventFromWatchEventKind(mockKind);
        assertThat(result, is(FileWatcher.Event.FILE_DELETED));
        result = FileWatcher.Event.eventFromWatchEventKind(mockKind);
        assertThat(result, is(FileWatcher.Event.FILE_MODIFIED));
    }
}