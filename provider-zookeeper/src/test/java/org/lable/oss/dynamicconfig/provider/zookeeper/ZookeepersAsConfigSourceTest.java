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
package org.lable.oss.dynamicconfig.provider.zookeeper;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.lable.oss.dynamicconfig.provider.zookeeper.ZookeepersAsConfigSource.nameToZnodeName;
import static org.lable.oss.dynamicconfig.provider.zookeeper.ZookeepersAsConfigSource.znodeNameToName;

public class ZookeepersAsConfigSourceTest {
//    @Ignore
//    @Test
//    public void testConstructorSlashPath() throws ConfigurationException {
//        ZookeepersAsConfigSource source = new ZookeepersAsConfigSource();
//        Configuration config = new BaseConfiguration();
//        config.setProperty("quorum", "QUORUM");
//        config.setProperty("znode", "/path/node");
//        config.setProperty(ConfigurationInitializer.APPNAME_PROPERTY, "my-app");
//        source.configure(config);
//
//        assertThat(source.znode, is("/path/node/my-app"));
//        assertThat(source.quorum.length, is(1));
//        assertThat(source.quorum[0], is("QUORUM"));
//    }

//    @Ignore
//    @Test
//    public void testConstructorWithAppNameAndQuorumList() throws ConfigurationException {
//        ZookeepersAsConfigSource source = new ZookeepersAsConfigSource();
//        Configuration config = new BaseConfiguration();
//        config.setProperty("quorum", "zk1,zk2");
//        config.setProperty("znode", "/path");
//        config.setProperty(ConfigurationInitializer.APPNAME_PROPERTY, "node");
//        source.configure(config);
//
//        assertThat(source.znode, is("/path/node"));
//        assertThat(source.quorum.length, is(2));
//        assertThat(source.quorum[0], is("zk1"));
//        assertThat(source.quorum[1], is("zk2"));
//    }

//    @Ignore
//    @Test(expected = ConfigurationException.class)
//    public void testConstructorNoAppName() throws ConfigurationException {
//        ZookeepersAsConfigSource source = new ZookeepersAsConfigSource();
//        Configuration config = new BaseConfiguration();
//        config.setProperty("quorum", "QUORUM");
//        config.setProperty("znode", "/path/node");
//        source.configure(config);
//    }

    @Test
    public void znodeNameToNameTest() {
        assertThat(znodeNameToName("/includes--conf.yaml"), is("includes/conf.yaml"));
        assertThat(znodeNameToName("/conf.yaml"), is("conf.yaml"));
    }

    @Test
    public void nameToZnodeNameTest() {
        assertThat(nameToZnodeName("includes/conf.yaml"), is("/includes--conf.yaml"));
        assertThat(nameToZnodeName("/includes/conf.yaml"), is("/includes--conf.yaml"));
        assertThat(nameToZnodeName("conf.yaml"), is("/conf.yaml"));
        assertThat(nameToZnodeName("/conf.yaml"), is("/conf.yaml"));
    }

}
