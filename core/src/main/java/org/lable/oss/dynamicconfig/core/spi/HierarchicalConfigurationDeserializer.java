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


import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;

import java.io.InputStream;

/**
 * Implementing classes can turn the serialized form of a {@link HierarchicalConfiguration} into a class instance.
 */
public interface HierarchicalConfigurationDeserializer {
    /**
     * Deserialize data representing a {@link HierarchicalConfiguration} into a class instance.
     *
     * @param input Input stream containing the raw serialized data.
     * @return A configuration instance.
     * @throws ConfigurationException Thrown when deserialization fails.
     */
    HierarchicalConfiguration deserialize(InputStream input) throws ConfigurationException;
}
