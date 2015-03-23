package org.lable.dynamicconfig.core.commonsconfiguration;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ObjectifierTest {
    @Test
    @SuppressWarnings("unchecked")
    public void testTree() throws Exception {

        // Prepare a configuration object to test with.

        HierarchicalConfiguration configuration = new HierarchicalConfiguration();
        configuration.setProperty("type.unicodeString", "€");
        configuration.setProperty("type.booleanFalse", false);
        configuration.setProperty("type.booleanTrue", true);
        configuration.setProperty("type.list", Arrays.asList("1", "2", "3"));
        configuration.setProperty("type.listInt", Arrays.asList(1, 2, 3));

        // Perform the test.

        Object tree = Objectifier.traverseTreeAndEmit(configuration.getRootNode());
        assertThat(tree, instanceOf(Map.class));

        Map<String, Object> treeMap = (Map<String, Object>) tree;
        assertThat(treeMap.get("type"), instanceOf(Map.class));

        Map<String, Object> typeMap = (Map<String, Object>) treeMap.get("type");
        assertThat(typeMap.size(), is(5));

        String unicodeString = (String) typeMap.get("unicodeString");
        assertThat(unicodeString, is("€"));

        boolean booleanFalse = (boolean) typeMap.get("booleanFalse");
        boolean booleanTrue = (boolean) typeMap.get("booleanTrue");
        assertThat(booleanFalse, is(false));
        assertThat(booleanTrue, is(true));

        List<String> stringList = (List<String>) typeMap.get("list");
        assertThat(stringList, is(Arrays.asList("1", "2", "3")));

        List<Integer> intList = (List<Integer>) typeMap.get("listInt");
        assertThat(intList, is(Arrays.asList(1, 2, 3)));
    }
}