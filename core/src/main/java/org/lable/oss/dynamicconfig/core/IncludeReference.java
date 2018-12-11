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

import java.util.Objects;

/**
 * A reference to another configuration resource.
 */
public class IncludeReference {
    private String name;
    private String configPath;

    public IncludeReference(String name) {
        this.name = name;
    }

    public IncludeReference(String configPath, String name) {
        this.configPath = configPath;
        this.name = name;
    }

    /**
     * The name of the referenced resource.
     *
     * @return A name (can be a path).
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * The location of the referenced resource in the resource referencing it.
     *
     * @return A dot-separated path.
     */
    public String getConfigPath() {
        return configPath;
    }

    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, configPath);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof IncludeReference)) return false;
        IncludeReference that = (IncludeReference) other;
        return Objects.equals(that.name, this.name) &&
                Objects.equals(that.configPath, this.configPath);
    }

    @Override
    public String toString() {
        return "Include: " + configPath + ": " + name;
    }
}
