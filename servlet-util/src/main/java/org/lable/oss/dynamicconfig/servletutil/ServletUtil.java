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
package org.lable.oss.dynamicconfig.servletutil;

import org.lable.oss.dynamicconfig.core.ConfigurationInitializer;
import org.lable.oss.dynamicconfig.core.InstanceLocalSettings;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

/**
 * Convenience methods for working with dynamic config from a servlet environment.
 */
public class ServletUtil {
    final static String APPNAME_PROPERTY = ConfigurationInitializer.LIBRARY_PREFIX + ".appname";

    /**
     * Set the context-path as application name.
     *
     * @param event ServletContextEvent.
     */
    public static void setApplicationNameFromContext(ServletContextEvent event) {
        setApplicationNameFromContext(event.getServletContext());
    }

    /**
     * Set the context-path as application name.
     *
     * @param context ServletContext.
     */
    public static void setApplicationNameFromContext(ServletContext context) {
        String contextPath = context.getContextPath();
        if (contextPath.isEmpty()) {
            // The empty string signifies the root context.
            contextPath = "ROOT";
        } else if (contextPath.startsWith("/")) {
            // Strip the leading / if any.
            contextPath = contextPath.substring(1);
        }

        InstanceLocalSettings.INSTANCE.setAppName(contextPath);
    }
}
