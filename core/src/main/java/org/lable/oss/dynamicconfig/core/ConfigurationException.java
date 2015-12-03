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
package org.lable.oss.dynamicconfig.core;

/**
 * Indicates a problem with the configuration process.
 */
public class ConfigurationException extends Exception {
    String parameter;

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String parameter, String message) {
        super(message);
        this.parameter = parameter;
    }

    public ConfigurationException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public String getParameter() {
        return parameter;
    }

    @Override
    public String getMessage() {
        return super.getMessage() +
                (getParameter() == null ? "" : "\nThis parameter caused this exception: " + getParameter());
    }
}
