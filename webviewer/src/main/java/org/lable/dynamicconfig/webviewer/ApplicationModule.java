package org.lable.dynamicconfig.webviewer;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.servlet.ServletModule;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.lable.dynamicconfig.webviewer.rest.SomeRestService;
import org.lable.dynamicconfig.webviewer.servlet.SomeServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ApplicationModule extends ServletModule {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationModule.class);

    @Override
    protected void configureServlets() {
        bind(SomeRestService.class);
        serve("/servlets/config/*").with(SomeServlet.class);
    }

    @Provides
    @Singleton
    public Configuration provideConfiguration() {
        logger.info("Providing configuration.");
        Configuration config = new HierarchicalConfiguration();
        config.setProperty("test", "ZZZ");
        return config;
    }
}