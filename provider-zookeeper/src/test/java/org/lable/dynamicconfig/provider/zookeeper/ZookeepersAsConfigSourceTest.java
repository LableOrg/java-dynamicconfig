package org.lable.dynamicconfig.provider.zookeeper;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.junit.Test;
import org.lable.dynamicconfig.core.ConfigurationException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

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
}
