package org.lable.dynamicconfig.provider.zookeeper;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.junit.Test;
import org.lable.dynamicconfig.core.ConfigurationException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.lable.dynamicconfig.provider.zookeeper.ZookeepersAsConfigSource.combinePath;

public class ZookeepersAsConfigSourceTest {
    @Test
    public void testConstructorSlashPath() throws ConfigurationException {
        ZookeepersAsConfigSource source = new ZookeepersAsConfigSource();
        Configuration config = new BaseConfiguration();
        config.setProperty("quorum", "QUORUM");
        config.setProperty("znode", "/path/node");
        source.configure(config);

        assertThat(source.znode, is("/path/node"));
        assertThat(source.quorum, is("QUORUM"));
    }

    @Test
    public void testConstructorWithAppName() throws ConfigurationException {
        ZookeepersAsConfigSource source = new ZookeepersAsConfigSource();
        Configuration config = new BaseConfiguration();
        config.setProperty("quorum", "QUORUM");
        config.setProperty("znode", "/path");
        config.setProperty("appname", "node");
        source.configure(config);

        assertThat(source.znode, is("/path/node"));
        assertThat(source.quorum, is("QUORUM"));
    }

    @Test
    public void testCombinePath() {
        assertThat(combinePath("path", "node"), is("path/node"));
        assertThat(combinePath("path", "/node"), is("path/node"));
        assertThat(combinePath("path/", "node"), is("path/node"));
        assertThat(combinePath("path/", "/node"), is("path/node"));
        assertThat(combinePath("path", null), is("path"));
        assertThat(combinePath("path", ""), is("path"));
    }
}
