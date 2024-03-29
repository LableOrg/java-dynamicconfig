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
package org.lable.oss.dynamicconfig.di;

import org.lable.oss.dynamicconfig.core.ConfigurationManager;

import java.io.Closeable;

/**
 * Consumers of this library can choose to implement this interface and bind it as an optional dependency to register
 * the shutdown hook of {@link ConfigurationManager}. If bound, {@link ConfigurationManager} will register itself via
 * {@link #register(Closeable)}.
 */
public interface ConfigurationManagerCloser {
    void register(Closeable closeable);
}
