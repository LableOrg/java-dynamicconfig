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
package org.lable.oss.dynamicconfig.core;

import org.junit.Test;
import org.lable.oss.dynamicconfig.core.spi.ConfigurationSource;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.lable.oss.dynamicconfig.core.ConfigurationManager.makeReferencesAbsolute;
import static org.lable.oss.dynamicconfig.core.ConfigurationManager.solveDots;

public class ConfigurationManagerTest {
    @Test
    public void detectServiceProvidersTest() {
        List<ConfigurationSource> result = ConfigurationManager.detectConfigurationSourceServiceProviders();

        assertThat(result.size(), is(2));
        assertThat(result.get(0).name(), is("file"));
        assertThat(result.get(1).name(), is("classpath"));
    }

    @Test
    public void makeReferencesAbsoluteTest() {
        List<IncludeReference> references = new ArrayList<>();
        references.add(new IncludeReference("a"));
        references.add(new IncludeReference("/a"));
        references.add(new IncludeReference("a/b"));
        references.add(new IncludeReference("/a/b"));
        references.add(new IncludeReference("../b"));
        references.add(new IncludeReference("../a/b"));
        references.add(new IncludeReference("./b"));
        references.add(new IncludeReference("/./b"));
        references.add(new IncludeReference("/one/./../b"));

        makeReferencesAbsolute("one/test/conf.yaml", references);

        assertThat(references.get(0).getName(), is("/one/test/a"));
        assertThat(references.get(1).getName(), is("/a"));
        assertThat(references.get(2).getName(), is("/one/test/a/b"));
        assertThat(references.get(3).getName(), is("/a/b"));
        assertThat(references.get(4).getName(), is("/one/b"));
        assertThat(references.get(5).getName(), is("/one/a/b"));
        assertThat(references.get(6).getName(), is("/one/test/b"));
        assertThat(references.get(7).getName(), is("/b"));
        assertThat(references.get(8).getName(), is("/b"));
    }

    @Test
    public void makeReferencesAbsoluteNoBaseDirTest() {
        List<IncludeReference> references = new ArrayList<>();
        references.add(new IncludeReference("a"));
        references.add(new IncludeReference("/a"));
        references.add(new IncludeReference("a/b"));

        makeReferencesAbsolute("conf.yaml", references);

        assertThat(references.get(0).getName(), is("/a"));
        assertThat(references.get(1).getName(), is("/a"));
        assertThat(references.get(2).getName(), is("/a/b"));

    }

    @Test
    public void solveDotsTest() {
        assertThat(solveDots(""), is(""));
        assertThat(solveDots("a.txt"), is("a.txt"));
        assertThat(solveDots("/a.txt"), is("/a.txt"));
        assertThat(solveDots("./a.txt"), is("a.txt"));
        assertThat(solveDots("/./a.txt"), is("/a.txt"));
        assertThat(solveDots("a/b/../x.txt"), is("a/x.txt"));
        assertThat(solveDots("a/b/../../x.txt"), is("x.txt"));
        assertThat(solveDots("a/b/../../../x.txt"), is(nullValue()));
    }
}