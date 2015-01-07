package org.lable.dynamicconfig.core;

import org.apache.commons.configuration.HierarchicalConfiguration;

/**
 * Callback for a changed configuration tree.
 */
public interface ConfigChangeListener {
    /**
     * Called when the configuration is mutated.
     *
     * @param fresh The new configuration tree.
     */
    public void changed(HierarchicalConfiguration fresh);
}