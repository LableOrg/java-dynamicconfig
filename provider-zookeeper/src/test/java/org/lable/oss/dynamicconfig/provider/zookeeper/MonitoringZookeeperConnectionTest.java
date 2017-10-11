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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.lable.oss.dynamicconfig.provider.zookeeper.MonitoringZookeeperConnection.nameToZnodeName;
import static org.lable.oss.dynamicconfig.provider.zookeeper.MonitoringZookeeperConnection.znodeNameToName;

public class MonitoringZookeeperConnectionTest {
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