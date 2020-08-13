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
     * <p>
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
