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