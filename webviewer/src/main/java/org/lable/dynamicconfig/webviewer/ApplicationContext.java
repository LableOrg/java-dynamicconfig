package org.lable.dynamicconfig.webviewer;

import org.jboss.resteasy.logging.Logger.LoggerType;
import org.jboss.resteasy.plugins.guice.GuiceResteasyBootstrapServletContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;


public class ApplicationContext extends GuiceResteasyBootstrapServletContextListener {
    private static Logger logger = LoggerFactory.getLogger(ApplicationContext.class);

    @Override
    public void contextInitialized(ServletContextEvent event) {
        logger.info("Initializing application context.");
        org.jboss.resteasy.logging.Logger.setLoggerType(LoggerType.SLF4J);
        super.contextInitialized(event);
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        super.contextDestroyed(event);
        logger.info("Application context destroyed.");
    }
}
