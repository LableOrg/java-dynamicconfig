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

import java.io.InputStream;

/**
 * Callback for a changed configuration tree.
 */
@FunctionalInterface
public interface ConfigChangeListener {
    /**
     * Called when a configuration part is mutated.
     *
     * @param name        Name of the configuration part.
     * @param inputStream Input stream containing the changed data.
     */
    void changed(String name, InputStream inputStream);
}