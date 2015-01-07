package org.lable.dynamicconfig.core;

/**
 * Indicates that a configuration parameter did not meet expectations.
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

    public String getParameter() {
        return parameter;
    }
}
