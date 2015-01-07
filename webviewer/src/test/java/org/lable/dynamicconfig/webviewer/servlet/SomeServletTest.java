package org.lable.dynamicconfig.webviewer.servlet;

import org.apache.commons.configuration.Configuration;
import org.hamcrest.MatcherAssert;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SomeServletTest {

    @Test
    public void testDoGet() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Configuration config = mock(Configuration.class);

        StringWriter sw = new StringWriter();
        PrintWriter writer = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(writer);

        when(config.getString("test")).thenReturn("AAA");
//        when(config.setProperty("test", anyString()));
//
//        verify(config.setProperty("test", anyString()));

        SomeServlet servlet = new SomeServlet(config);

        servlet.doGet(request, response);

        System.out.println(sw.getBuffer());
    }

    @Test
    public void testGetFirstPathPartNullInput() {
        MatcherAssert.assertThat(SomeServlet.getFirstPathPart(null), is(nullValue()));
    }

    @Test
    public void testGetFirstPathPartNoParts() {
        MatcherAssert.assertThat(SomeServlet.getFirstPathPart("/"), is(nullValue()));
    }

    @Test
    public void testGetFirstPathEmpty() {
        MatcherAssert.assertThat(SomeServlet.getFirstPathPart(""), is(nullValue()));
    }

    @Test
    public void testGetFirstPathInvalidStartChar() {
        MatcherAssert.assertThat(SomeServlet.getFirstPathPart("xxx"), is(nullValue()));
        MatcherAssert.assertThat(SomeServlet.getFirstPathPart("xxx/yyy"), is(nullValue()));
    }

    @Test
    public void testGetFirstPathValid() {
        MatcherAssert.assertThat(SomeServlet.getFirstPathPart("/xxx"), is("xxx"));
        MatcherAssert.assertThat(SomeServlet.getFirstPathPart("/xxx/yyy"), is("xxx/yyy"));
    }
}