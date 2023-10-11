package org.apache.sling.testing.mock.osgi.config;

import org.apache.felix.scr.impl.inject.Annotations;
import org.apache.sling.testing.mock.osgi.MapUtil;
import org.apache.sling.testing.mock.osgi.config.annotations.ApplyConfig;
import org.apache.sling.testing.mock.osgi.config.annotations.UpdateConfig;
import org.apache.sling.testing.mock.osgi.context.OsgiContextImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Performs configuration management and component property type construction for
 * {@link org.apache.sling.testing.mock.osgi.config.annotations.ApplyConfig} and
 * {@link org.apache.sling.testing.mock.osgi.config.annotations.UpdateConfig} annotations.
 */
public class ConfigTypeContext {
    private final OsgiContextImpl osgiContext;

    public ConfigTypeContext(OsgiContextImpl osgiContext) {
        this.osgiContext = osgiContext;
    }

    /**
     * Internal utility method to update a configuration with properties from the provided map.
     * @param pid the configuration pid
     * @param configurationAdmin a config admin service
     * @param updatedProperties a map of properties to update the configuration
     * @throws RuntimeException if an IOException is thrown by {@link ConfigurationAdmin}
     */
    public static void updatePropertiesForConfigPid(@NotNull Map<String, Object> updatedProperties,
                                                    @NotNull String pid,
                                                    @Nullable ConfigurationAdmin configurationAdmin) {
        if (configurationAdmin != null) {
            try {
                Configuration configuration = configurationAdmin.getConfiguration(pid);
                configuration.update(MapUtil.toDictionary(updatedProperties));
            } catch (IOException e) {
                throw new IllegalStateException("Unable to read/write config for pid " + pid, e);
            }
        }
    }

    /**
     * Internal utility method to merge properties from the specified configuration into the provided map.
     * @param pid the configuration pid
     * @param configurationAdmin a config admin service
     * @param mergedProperties a *mutable* map
     * @throws RuntimeException if an IOException is thrown by {@link ConfigurationAdmin#getConfiguration(String)}
     * @throws UnsupportedOperationException if an immutable map is passed
     */
    public static void mergePropertiesFromConfigPid(@NotNull Map<String, Object> mergedProperties,
                                                    @NotNull String pid,
                                                    @Nullable ConfigurationAdmin configurationAdmin) {
        if (configurationAdmin != null) {
            try {
                Configuration configuration = configurationAdmin.getConfiguration(pid);
                Optional.ofNullable(MapUtil.toMap(configuration.getProperties())).ifPresent(mergedProperties::putAll);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to read config for pid " + pid, e);
            }
        }
    }

    /**
     * Construct a configuration pid for use with {@link ConfigurationAdmin#getConfiguration(String)}.
     * If {@code pid} is empty, return {@link Optional#empty()}. If {@code pid} is equal to {@link Component#NAME} ("$"),
     * return {@code component.getName()}.
     *
     * @param pid an explicit pid name, "$", or the empty string
     * @param component a class whose name to use when pid is "$"
     * @return a useful configuration pid or none
     */
    public static Optional<String> getConfigurationPid(@NotNull final String pid, @NotNull final Class<?> component) {
        if (pid.isEmpty()) {
            return Optional.empty();
        } else if (Component.NAME.equals(pid)) {
            return Optional.of(component.getName());
        } else {
            return Optional.of(pid);
        }
    }

    /**
     * Updates a {@link Configuration} from the provided annotation.
     * @param annotation an {@link UpdateConfig} annotation
     */
    public final void updateConfiguration(@NotNull final UpdateConfig annotation) {
        getConfigurationPid(annotation.pid(), annotation.component()).ifPresent(pid -> {
            final Map<String, Object> updated = ComponentPropertyParser.parse(annotation.property());
            updatePropertiesForConfigPid(updated, pid, osgiContext.getService(ConfigurationAdmin.class));
        });
    }

    /**
     * Return a concrete instance of the OSGi config / Component Property Type represented by the given
     * {@link ApplyConfig} annotation discovered via reflection.
     * @param annotation the {@link ApplyConfig}
     * @return a concrete instance of the type specified by the provided {@link ApplyConfig#type()}
     */
    public final Object constructComponentPropertyType(@NotNull final ApplyConfig annotation) {
        return constructComponentPropertyType(annotation, null);
    }

    /**
     * Return a concrete instance of the OSGi config / Component Property Type represented by the given
     * {@link ApplyConfig} annotation discovered via reflection.
     * @param annotation the {@link ApplyConfig}
     * @param applyPid if not empty, override any specified {@link ApplyConfig#pid()}.
     * @return a concrete instance of the type specified by the provided {@link ApplyConfig#type()}
     */
    public final Object constructComponentPropertyType(@NotNull final ApplyConfig annotation,
                                                       @Nullable final String applyPid) {
        if (!annotation.type().isAnnotation() && !annotation.type().isInterface()) {
            throw new IllegalArgumentException("illegal value for ApplyConfig " + annotation.type());
        }
        final Map<String, Object> merged = new HashMap<>(
                ComponentPropertyParser.parse(annotation.type(), annotation.property()));
        Optional.ofNullable(applyPid).filter(pid -> !pid.isEmpty())
                .or(() -> getConfigurationPid(annotation.pid(), annotation.component()))
                .ifPresent(pid -> mergePropertiesFromConfigPid(merged, pid, osgiContext.getService(ConfigurationAdmin.class)));
        return Annotations.toObject(annotation.type(), merged, osgiContext.bundleContext().getBundle(), false);
    }
}
