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

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.junit.Test;
import org.lable.oss.dynamicconfig.core.ConfigurationComposition.ConfigReference;
import org.lable.oss.dynamicconfig.core.ConfigurationComposition.ConfigState;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ConfigurationCompositionTest {
    @Test
    public void compositionTest() {
        ConfigurationComposition composition = new ConfigurationComposition();
        composition.setRootReference(composition.updateReferences(
                "root", Arrays.asList(
                        new IncludeReference("b-a.b-a", "a"),
                        new IncludeReference("b-a.b-b", "b"),
                        new IncludeReference("b-b.b-a", "c"),
                        new IncludeReference("b-b.b-b", "d")
                )
        ));

        composition.updateReferences("b", Arrays.asList(
                new IncludeReference("b-a.b.b.1", "e1"),
                new IncludeReference("b-a.b.b.2", "e2")
        ));

        assertThat(composition.allReferences.size(), is(7));
        assertThat(references(composition, "root", "a"), is(true));
        assertThat(references(composition, "root", "b"), is(true));
        assertThat(references(composition, "b", "e1"), is(true));
        assertThat(references(composition, "b", "e2"), is(true));
        assertThat(references(composition, "root", "c"), is(true));
        assertThat(references(composition, "root", "d"), is(true));

        composition.updateReferences("b", Collections.emptyList());

        assertThat(references(composition, "root", "a"), is(true));
        assertThat(references(composition, "root", "b"), is(true));
        assertThat(references(composition, "b", "e1"), is(false));
        assertThat(references(composition, "b", "e2"), is(false));
        assertThat(references(composition, "root", "c"), is(true));
        assertThat(references(composition, "root", "d"), is(true));

        composition.updateReferences("a", Arrays.asList(
                new IncludeReference("b-a.b.a.1", "e1"),
                new IncludeReference("b-a.b.a.2", "e2")
        ));

        composition.updateReferences("c", Arrays.asList(
                new IncludeReference("b-b.b.b.1", "e1"),
                new IncludeReference("b-b.b.b.2", "e2")
        ));

        assertThat(references(composition, "root", "a"), is(true));
        assertThat(references(composition, "a", "e1"), is(true));
        assertThat(references(composition, "a", "e2"), is(true));
        assertThat(references(composition, "root", "b"), is(true));
        assertThat(references(composition, "root", "c"), is(true));
        assertThat(references(composition, "c", "e1"), is(true));
        assertThat(references(composition, "c", "e2"), is(true));
        assertThat(references(composition, "root", "d"), is(true));
    }

    @Test
    public void compositionMetaStatusTest() {
        ConfigurationComposition composition = new ConfigurationComposition();
        composition.setRootReference(composition.updateReferences(
                "root", Arrays.asList(
                        new IncludeReference("aaa", "a"),
                        new IncludeReference("bbb", "b")
                )
        ));

        Configuration configuration = new BaseConfiguration();
        composition.setMetadata(configuration);

        assertThat(
                configuration.getString(ConfigurationComposition.META_MODIFIED_TREE),
                is(
                        ". -> root                                                    (PENDING @ -)\n" +
                        "aaa -> a                                                     (PENDING @ -)\n" +
                        "bbb -> b                                                     (PENDING @ -)\n"
                )
        );
    }

    @Test
    public void orphanageTest() {
        ConfigurationComposition composition = new ConfigurationComposition();
        composition.setRootReference(composition.updateReferences(
                "root", Arrays.asList(
                        new IncludeReference("b-a.b-a", "a"),
                        new IncludeReference("b-a.b-b", "b"),
                        new IncludeReference("b-b.b-a", "c"),
                        new IncludeReference("b-b.b-b", "d")
                )
        ));

        composition.updateReferences("b", Arrays.asList(
                new IncludeReference("b-a.b.b.1", "e1"),
                new IncludeReference("b-a.b.b.2", "e2")
        ));

        composition.updateReferences("e2", Arrays.asList(
                new IncludeReference("b-a.b.b.1", "e2.1"),
                new IncludeReference("b-a.b.b.2", "e2.2")
        ));

        composition.updateReferences("a", Arrays.asList(
                new IncludeReference("b-a.b.a.1", "e1"),
                new IncludeReference("b-a.b.a.2", "e3")
        ));

        composition.updateReferences("b", Arrays.asList(
                new IncludeReference("b2", "e1"),
                new IncludeReference("b3", "e1")
        ));

        assertThat(composition.getReference("e2").getConfigState(), is(ConfigState.ORPHANED));
        assertThat(composition.getReference("e2.1").getConfigState(), is(ConfigState.ORPHANED));
        assertThat(composition.getReference("e2.2").getConfigState(), is(ConfigState.ORPHANED));

        composition.getRidOfOrphans();

        assertThat(composition.allReferences.containsKey("e2"), is(false));
        assertThat(composition.allReferences.containsKey("e2.1"), is(false));
        assertThat(composition.allReferences.containsKey("e2.2"), is(false));
    }

    /**
     * Assert that two configuration parts reference each other in both directions (one referencing, and one knowing
     * that it is being referenced).
     *
     * @param composition    Configuration composition.
     * @param referencerName Referencer.
     * @param referenceeName Referencee.
     * @return True if the two parts reference each other properly.
     */
    static boolean references(ConfigurationComposition composition, String referencerName, String referenceeName) {
        ConfigReference referencer = composition.allReferences.get(referencerName);
        ConfigReference referencee = composition.allReferences.get(referenceeName);

        return referencer != null && referencee != null &&
                referencer.referencedByMe.containsValue(referencee) &&
                referencee.referencingMe.contains(referencer);
    }
}