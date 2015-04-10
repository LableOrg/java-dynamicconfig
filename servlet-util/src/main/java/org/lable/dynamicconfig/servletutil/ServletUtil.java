package org.lable.dynamicconfig.servletutil;

import org.lable.dynamicconfig.core.ConfigurationInitializer;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import static org.apache.commons.lang.StringUtils.isBlank;

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
        if (contextPath.equals("")) {
            // The empty string signifies the root context.
            contextPath = "ROOT";
        }

        // Only set this property if it is not already configured.
        if (isBlank(System.getProperty(APPNAME_PROPERTY))) {
            System.setProperty(APPNAME_PROPERTY, contextPath);
        }
    }
}
