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
package org.lable.oss.dynamicconfig.configutil;

import org.apache.commons.configuration.Configuration;

import java.util.*;

/**
 * Static convenience methods for working with {@link Configuration} instances.
 */
public class ConfigUtil {
    ConfigUtil() {
        // No-op.
    }

    /**
     * Get a map of configuration nodes mapped to the names of the children of the parent parameter.
     *
     * @param config Configuration object, may be a subset of the configuration tree.
     * @param parent Path to the parent node.
     * @return A map of all the children for the parent configuration node passed.
     */
    public static Map<String, Configuration> childMap(Configuration config, String parent) {
        if (config == null) throw new IllegalArgumentException("Parameter config may not be null.");

        if (parent != null && !parent.isEmpty()) {
            config = config.subset(parent);
        }

        Iterator<String> keys = config.getKeys();
        Map<String, Configuration> map = new LinkedHashMap<>();
        while (keys.hasNext()) {
            String key = keys.next();
            key = key.split("\\.", 2)[0];
            if (!map.containsKey(key)) {
                map.put(key, config.subset(key));
            }
        }
        return map;
    }

    /**
     * Get a map of configuration nodes mapped to the names of the children of the root configuration node.
     *
     * @param config Configuration object, may be a subset of the configuration tree.
     * @return A map of all the children for the parent configuration node passed.
     */
    public static Map<String, Configuration> childMap(Configuration config) {
        return childMap(config, null);
    }

    /**
     * Get the list of child keys for a configuration node.
     *
     * @param config Configuration object, may be a subset of the configuration tree.
     * @param parent Path to the parent node.
     * @return A string array containing the names of the children.
     */
    public static Set<String> childKeys(Configuration config, String parent) {
        if (config == null) throw new IllegalArgumentException("Parameter config may not be null.");

        if (parent != null && !parent.isEmpty()) {
            config = config.subset(parent);
        }

        Iterator keys = config.getKeys();
        Set<String> list = new HashSet<>();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            key = key.split("\\.", 2)[0];
            list.add(key);
        }
        return list;
    }


    /**
     * Get the list of child keys for a configuration node.
     *
     * @param config Configuration object, may be a subset of the configuration tree.
     * @return A string array containing the names of the children.
     */
    public static Set<String> childKeys(Configuration config) {
        return childKeys(config, null);
    }
}
