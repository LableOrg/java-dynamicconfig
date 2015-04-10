package org.lable.dynamicconfig.servletutil;

import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.lable.dynamicconfig.servletutil.ServletUtil.APPNAME_PROPERTY;
import static org.lable.dynamicconfig.servletutil.ServletUtil.setApplicationNameFromContext;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServletUtilTest {
    @Before
    public void before() {
        System.clearProperty(APPNAME_PROPERTY);
    }

    @Test
    public void setApplicationNameFromContextTest() {
        ServletContext context = mock(ServletContext.class);
        when(context.getContextPath()).thenReturn("TEST");

        setApplicationNameFromContext(context);

        assertThat(System.getProperty(APPNAME_PROPERTY), is("TEST"));
    }

    @Test
    public void setApplicationNameFromContextEventTest() {
        ServletContextEvent event = mock(ServletContextEvent.class);
        ServletContext context = mock(ServletContext.class);
        when(event.getServletContext()).thenReturn(context);
        when(context.getContextPath()).thenReturn("TEST");

        setApplicationNameFromContext(context);

        assertThat(System.getProperty(APPNAME_PROPERTY), is("TEST"));
    }
}