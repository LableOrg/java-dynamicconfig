/**
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
package org.lable.oss.dynamicconfig.core.spi;


import org.apache.commons.configuration.HierarchicalConfiguration;
import org.lable.oss.dynamicconfig.core.ConfigurationException;

import java.io.OutputStream;

/**
 * Implementing classes can serialize a {@link HierarchicalConfiguration} class instance.
 */
public interface HierarchicalConfigurationSerializer {
    /**
     * Serialize a {@link HierarchicalConfiguration} class instance into its binary representation.
     *
     * @param configuration Configuration instance.
     * @param output Output stream where the serialized data will be written to.
     * @throws ConfigurationException Thrown when serialization fails.
     */
    void serialize(HierarchicalConfiguration configuration, OutputStream output) throws ConfigurationException;
}
