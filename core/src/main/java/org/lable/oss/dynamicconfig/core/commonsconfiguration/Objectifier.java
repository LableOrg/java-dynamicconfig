package org.lable.oss.dynamicconfig.core.commonsconfiguration;

import org.apache.commons.configuration.tree.ConfigurationNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class that converts {@link org.apache.commons.configuration.tree.ConfigurationNode} instances to a plain
 * Java object tree.
 */
public class Objectifier {

    /**
     * Process a node in the Config tree, and store it with its parent node in an object tree.
     * <p/>
     * This method recursively calls itself to walk a Config tree.
     *
     * @param parent Parent of the current node, as represented in the Config tree.
     * @return An object tree.
     */
    public static Object traverseTreeAndEmit(ConfigurationNode parent) {
        if (parent.getChildrenCount() == 0) {
            return parent.getValue();
        } else {
            Map<String, Object> map = new LinkedHashMap<>();
            for (Object o : parent.getChildren()) {
                ConfigurationNode child = (ConfigurationNode) o;
                String nodeName = child.getName();
                addToMap(map, nodeName, traverseTreeAndEmit(child));
            }
            return map;
        }
    }

    @SuppressWarnings("unchecked")
    static void addToMap(Map<String, Object> map, String nodeName, Object value) {
        if (map.containsKey(nodeName)) {
            // Trying to add an item with a key that is already used. This means we are
            // dealing with a list.
            Object existing = map.get(nodeName);
            List<Object> list;
            if (existing instanceof List) {
                // Unchecked cast, but correct.
                list = (List<Object>) existing;
            } else {
                list = new ArrayList<>();
                list.add(existing);
            }
            list.add(value);
            map.put(nodeName, list);
        } else {
            map.put(nodeName, value);
        }
    }
}
