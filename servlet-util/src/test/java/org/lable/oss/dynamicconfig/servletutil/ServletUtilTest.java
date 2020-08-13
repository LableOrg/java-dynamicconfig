/*
 * Copyright © 2015 Lable (info@lable.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lable.oss.dynamicconfig.servletutil;

import org.junit.Before;
import org.junit.Test;
import org.lable.oss.dynamicconfig.core.InstanceLocalSettings;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.lable.oss.dynamicconfig.servletutil.ServletUtil.APPNAME_PROPERTY;
import static org.lable.oss.dynamicconfig.servletutil.ServletUtil.setApplicationNameFromContext;
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

        assertThat(InstanceLocalSettings.INSTANCE.getAppName(), is("TEST"));
    }

    @Test
    public void setApplicationNameFromContextEventTest() {
        ServletContextEvent event = mock(ServletContextEvent.class);
        ServletContext context = mock(ServletContext.class);
        when(event.getServletContext()).thenReturn(context);
        when(context.getContextPath()).thenReturn("TEST");

        setApplicationNameFromContext(context);

        assertThat(InstanceLocalSettings.INSTANCE.getAppName(), is("TEST"));
    }
}