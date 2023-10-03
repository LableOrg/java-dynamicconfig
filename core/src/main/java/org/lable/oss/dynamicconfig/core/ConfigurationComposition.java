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
package org.lable.oss.dynamicconfig.core;

import org.apache.commons.configuration.CombinedConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;

import java.time.Instant;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Representation of a tree of configuration parts.
 */
public class ConfigurationComposition {
    Map<String, ConfigReference> allReferences;
    ConfigReference root;
    HierarchicalConfiguration defaultConfiguration;

    /**
     * Internal name used for the special default configuration that can be provided hard-coded.
     */
    private static final String DEFAULT_CONFIG_NAME = "--------";

    public static final String META_MODIFIED_AT = "dc.meta.last-update";

    public static final String META_MODIFIED_TREE = "dc.meta.tree-state";

    /**
     * Create a new empty {@link ConfigurationComposition}.
     */
    public ConfigurationComposition() {
        this(null);
    }

    /**
     * Create a new {@link ConfigurationComposition}, adding a special non-dynamic configuration part that will be
     * used as the default (the combined configuration will extend from it).
     *
     * @param defaultConfiguration Default configuration.
     */
    public ConfigurationComposition(HierarchicalConfiguration defaultConfiguration) {
        reset(defaultConfiguration);
    }

    public void reset(HierarchicalConfiguration defaultConfiguration) {
        this.allReferences = new HashMap<>();
        this.defaultConfiguration = defaultConfiguration;
        this.root = null;
    }

    /**
     * Mark a configuration reference as root of this composition.
     *
     * @param rootReference Configuration reference.
     */
    void setRootReference(ConfigReference rootReference) {
        extendFromDefaults(rootReference);
        this.root = rootReference;
    }

    /**
     * Update all references made by the named configuration part. If the name of a reference represents a path, it
     * is expected to be relative to the root configuration reference. A leading {@code /} is permitted, but will be
     * stripped from the reference name.
     *
     * @param name          Configuration part.
     * @param newReferences The part's new references.
     * @return The named configuration part's {@link ConfigReference}.
     */
    synchronized ConfigReference updateReferences(String name, List<IncludeReference> newReferences) {
        ConfigReference current = allReferences.computeIfAbsent(name, ConfigReference::new);
        // Unlink all existing references for this ConfigReference.
        final Set<ConfigReference> dereferenced = current.unlinkAllReferences();

        newReferences.stream()
                // Strip any leading '/' in references.
                .peek(ir -> {
                    if (ir.getName().startsWith("/")) ir.setName(ir.getName().substring(1));
                })
                // Filter out references that are already referencing us. If we don't, we will
                // end up with circular references.
                .filter(ir -> !current.hasReferenceInChain(ir.getName()))
                .forEach(ir -> {
                    ConfigReference referencee = allReferences.computeIfAbsent(ir.getName(), ConfigReference::new);
                    current.addReciprocalReference(ir, referencee);
                    allReferences.put(referencee.getName(), referencee);
                    dereferenced.remove(referencee);
                });

        // Get rid of all orphaned configuration parts.
        Set<ConfigReference> orphans = findDereferencedConfigReferences(dereferenced);
        orphans.forEach(ConfigReference::markAsOrphaned);

        if (current == root) extendFromDefaults(current);

        return current;
    }

    synchronized void setConfigurationOnReference(ConfigReference reference,
                                                         HierarchicalConfiguration configuration) {
        reference.setConfiguration(configuration);
        reference.markTimeOfUpdate();
    }

    synchronized ConfigReference markReferenceAsFailedToLoad(String name) {
        ConfigReference current = allReferences.computeIfAbsent(name, ConfigReference::new);
        current.markAsFailedToLoad();
        return current;
    }

    synchronized void markReferenceAsNeedsLoading(String name) {
        ConfigReference current = allReferences.computeIfAbsent(name, ConfigReference::new);
        current.markAsNeedsLoading();
    }

    synchronized boolean hasMatchingReference(String name, Predicate<ConfigReference> filter) {
        return allReferences.containsKey(name) && filter.test(allReferences.get(name));
    }

    synchronized ConfigReference getReference(String name) {
        return allReferences.get(name);
    }

    synchronized List<ConfigReference> getReferences(Predicate<ConfigReference> filter) {
        return allReferences.values().stream().filter(filter).collect(Collectors.toList());
    }

    public synchronized void getRidOfOrphans() {
        allReferences.values().removeIf(ref -> ref.getConfigState() == ConfigState.ORPHANED);
    }

    /**
     * Assemble the full configuration as represented by this composition.
     *
     * @param combinedConfig Configuration object to place the configuration in.
     */
    public synchronized void assembleConfigTree(CombinedConfiguration combinedConfig) {
        combinedConfig.clear();
        assembleConfigSection(combinedConfig);
        setMetadata(combinedConfig);
    }

    /**
     * Recursively assemble the combined configuration.
     *
     * @param combinedConfig  Configuration object to write the configuration to.
     */
    void assembleConfigSection(CombinedConfiguration combinedConfig) {
        walk((path, ref) -> {
            // Give every configuration part a unique name.
            String name = (path == null ? "" : path) + "->" + ref.getName();

            if (ref.configuration != null) {
                combinedConfig.addConfiguration(ref.configuration, name, path);
            }
        });
    }

    void setMetadata(Configuration combinedConfig) {
        combinedConfig.setProperty(META_MODIFIED_AT, Instant.now().toString());

        StringBuilder builder = new StringBuilder();

        walk((path, ref) -> {
            // Ignore the default configuration; it is never updated and hard-coded.
            if (path == null && ref.getName().equals(DEFAULT_CONFIG_NAME)) return;

            String name = (path == null ? "." : path) + " -> " + ref.getName();
            builder.append(name);


            builder.append(" ".repeat(Math.max(0, 60 - name.length())));
            String updated = ref.getLastUpdated().map(Instant::toString).orElse("-");
            builder.append(" (")
                    .append(ref.configState)
                    .append(" @ ")
                    .append(updated)
                    .append(")\n");
        });

        combinedConfig.setProperty(META_MODIFIED_TREE, builder.toString());
    }

    /**
     * Walk the tree of configuration parts and call a supplied method for each part.
     *
     * @param referenceConsumer Callback for each configuration reference visited.
     */
    void walk(BiConsumer<String, ConfigReference> referenceConsumer) {
        walk(null, root, referenceConsumer);
    }

    /**
     * Walk the tree of configuration parts and call a supplied method for each part.
     *
     * @param path              Path in the configuration object of the current configuration reference.
     *                          May be {@code null} for the root path.
     * @param configReference   Current configuration reference.
     * @param referenceConsumer Callback for each configuration reference visited.
     */
    void walk(String path, ConfigReference configReference, BiConsumer<String, ConfigReference> referenceConsumer) {
        referenceConsumer.accept(path, configReference);

        // Includes (configPath is set). These take precedence over the configuration parts that are extended.
        configReference.referencedByMe.forEach((includeReference, reference) -> {
            if (includeReference.getConfigPath() == null) return;
            String newPath = path == null
                    ? includeReference.getConfigPath()
                    : path + "." + includeReference.getConfigPath();
            walk(newPath, reference, referenceConsumer);
        });

        // Extends (configPath is not set).
        configReference.referencedByMe.forEach((includeReference, reference) -> {
            if (includeReference.getConfigPath() != null) return;
            walk(path, reference, referenceConsumer);
        });
    }

    /**
     * Make the referenced configuration part extend from the default configuration (if set).
     *
     * @param reference Configuration reference.
     */
    void extendFromDefaults(ConfigReference reference) {
        // Only if a default configuration was passed to us.
        if (defaultConfiguration == null) return;

        ConfigReference defaults = allReferences.computeIfAbsent(DEFAULT_CONFIG_NAME, ConfigReference::new);
        defaults.setConfiguration(defaultConfiguration);
        IncludeReference ir = new IncludeReference(DEFAULT_CONFIG_NAME);
        reference.addReciprocalReference(ir, defaults);
    }

    /**
     * Find out if a configuration part has become orphaned. An orphan is a part that is referenced by none. It can
     * have references of its own; these will be inspected and added to the set of orphans if they
     *
     * @param potentialOrphans A collection of potential orphans.
     * @return All actually orphaned references.
     */
    Set<ConfigReference> findDereferencedConfigReferences(Collection<ConfigReference> potentialOrphans) {
        // If a configuration part is still referenced by other parts, it is not orphaned.
        potentialOrphans.removeIf(reference -> !reference.referencingMe.isEmpty());

        // The root reference is a special case that is not referenced itself (being the root),
        // but should not be considered an orphan.
        potentialOrphans.removeIf(reference -> reference.getName().equals(root.getName()));

        Set<ConfigReference> orphans = new HashSet<>(potentialOrphans);

        // Unlink the references of each orphan, and recurse into those (former) references to see if they
        // in turn became orphans themselves.
        for (ConfigReference orphan : potentialOrphans) {
            Set<ConfigReference> formerReferencesOfOrphan = orphan.unlinkAllReferences();
            orphans.addAll(findDereferencedConfigReferences(formerReferencesOfOrphan));
        }

        return orphans;
    }

    @Override
    public String toString() {
        if (root == null) return "(empty)";

        StringBuilder builder = new StringBuilder();
        nodeToString(builder, root, 0);
        return builder.toString();
    }

    private void nodeToString(StringBuilder builder, ConfigReference node, int depth) {
        builder.append("  ".repeat(Math.max(0, depth)));
        builder.append(node.getName()).append('\n');

        for (ConfigReference reference : node.referencedByMe.values()) {
            nodeToString(builder, reference, depth + 1);
        }
    }

    /**
     * A part of the configuration composition. Each part holds its own part of the configuration, and maintains a
     * bidirectional mapping of configuration parts it references, and those that reference it.
     */
    static class ConfigReference {
        Set<ConfigReference> referencingMe;
        Map<IncludeReference, ConfigReference> referencedByMe;
        String name;
        ConfigState configState;
        Instant lastUpdated;
        HierarchicalConfiguration configuration;

        ConfigReference(String name) {
            this.name = name;
            this.referencedByMe = new HashMap<>();
            this.referencingMe = new HashSet<>();
            this.configState = ConfigState.NEEDS_LOADING;
            this.configuration = null;
            this.lastUpdated = null;
        }

        void addReciprocalReference(IncludeReference includeReference, ConfigReference referencee) {
            // Link referencer and referencee.
            referencedByMe.put(includeReference, referencee);
            referencee.referencingMe.add(this);
        }

        void removeReferenceToMe(ConfigReference referencer) {
            referencingMe.remove(referencer);
        }

        void stopReferencing(ConfigReference referencee) {
            referencedByMe.values().remove(referencee);
        }

        Set<ConfigReference> unlinkAllReferences() {
            Set<ConfigReference> unlinkedReferences = new HashSet<>();
            referencedByMe.values().stream().distinct().forEach(referencee -> {
                unlinkedReferences.add(referencee);
                referencee.removeReferenceToMe(this);
            });
            referencedByMe.clear();
            return unlinkedReferences;
        }

        public String getName() {
            return name;
        }

        void setConfiguration(HierarchicalConfiguration configuration) {
            this.configuration = configuration;
            this.configState = ConfigState.LOADED;
        }

        void markTimeOfUpdate() {
            this.lastUpdated = Instant.now();
        }

        void markAsFailedToLoad() {
            this.configState = ConfigState.FAILED_TO_LOAD;
        }

        void markAsNeedsLoading() {
            this.configState = ConfigState.NEEDS_LOADING;
        }

        void markAsOrphaned() {
            this.configState = ConfigState.ORPHANED;
        }

        public ConfigState getConfigState() {
            return configState;
        }

        public Optional<Instant> getLastUpdated() {
            return Optional.ofNullable(lastUpdated);
        }

        boolean hasReferenceInChain(String name) {
            if (this.name.equals(name)) return true;

            for (ConfigReference configReference : referencingMe) {
                if (configReference.hasReferenceInChain(name)) return true;
            }

            return false;
        }

        public String toString() {
            StringBuilder builder = new StringBuilder(name);
            builder.append(", includes: ");
            if (referencedByMe.isEmpty()) {
                builder.append(" -");
            } else {
                builder.append(referencedByMe.values().stream()
                        .map(ConfigReference::getName)
                        .collect(Collectors.joining(", "))
                );
            }

            return builder.toString();
        }
    }

    enum ConfigState {
        NEEDS_LOADING("PENDING"),
        LOADED("OK"),
        FAILED_TO_LOAD("FAILED"),
        ORPHANED("ORPHANED");

        private final String state;

        ConfigState(String state) {
            this.state = state;
        }

        @Override
        public String toString() {
            return state;
        }
    }
}
