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
package org.lable.oss.dynamicconfig.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Process-local settings for bootstrapping this library.
 */
public enum InstanceLocalSettings {
    INSTANCE;

    private String name;
    private String rootPath;
    private Map<MetaDataKey, String> metaData;

    InstanceLocalSettings() {
        name = null;
        metaData = new HashMap<>();
    }

    public void setAppName(String name) {
        this.name = name;
    }

    public String getAppName() {
        return name;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public String getRootPath() {
        return rootPath;
    }

    public void setMetaData(String category, String key, String value) {
        metaData.put(new MetaDataKey(category, key), value);
    }

    public Optional<String> getMetaData(String category, String key) {
        MetaDataKey mdk = new MetaDataKey(category, key);
        return Optional.ofNullable(metaData.get(mdk));
    }

    public Map<MetaDataKey, String> getMetaData(String category) {
        return metaData.entrySet().stream()
                .filter((entry) -> entry.getKey().getCategory().equals(category))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public Map<MetaDataKey, String> getMetaData() {
        return metaData;
    }

    /**
     * Category-key combination that is used as key for meta data in the InstanceLocalSettings.
     */
    public static class MetaDataKey {

        private String category;
        private String key;

        MetaDataKey(String category, String key) {
            if (category == null) throw new IllegalArgumentException("Category may not be null.");
            if (key == null) throw new IllegalArgumentException("Key may not be null.");

            this.category = category;
            this.key = key;
        }

        public String getCategory() {
            return category;
        }

        public String getKey() {
            return key;
        }

        @Override
        public int hashCode() {
            return Objects.hash(category, key);
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof MetaDataKey)) return false;
            MetaDataKey that = (MetaDataKey) other;
            return Objects.equals(this.category, that.category) && Objects.equals(this.key, that.key);
        }

        @Override
        public String toString() {
            return category + ":" + key;
        }
    }
}
