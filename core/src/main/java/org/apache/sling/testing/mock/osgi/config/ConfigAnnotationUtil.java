/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.testing.mock.osgi.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.sling.testing.mock.osgi.config.annotations.AutoConfig;
import org.apache.sling.testing.mock.osgi.config.annotations.ConfigCollection;
import org.apache.sling.testing.mock.osgi.config.annotations.ConfigType;
import org.apache.sling.testing.mock.osgi.config.annotations.ConfigTypes;
import org.apache.sling.testing.mock.osgi.config.annotations.SetConfig;
import org.apache.sling.testing.mock.osgi.config.annotations.SetConfigs;
import org.apache.sling.testing.mock.osgi.config.annotations.TypedConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Common functions for resolving OSGi config test parameters.
 */
public final class ConfigAnnotationUtil {
    private static final Set<Class<? extends Annotation>> EXCLUDE_FEATURE_ANNOTATIONS =
            Set.of(ConfigTypes.class, ConfigType.class, SetConfigs.class, SetConfig.class, AutoConfig.class);

    private ConfigAnnotationUtil() {
        // prevent instantiation
    }

    /**
     * Predicate for custom filtering of fully identified candidate annotations, accepting an {@link Optional} of
     * {@link ConfigType} representing the parent annotation when present, and the {@link Class} of the effective config
     * type.
     */
    @FunctionalInterface
    public interface ConfigTypePredicate extends BiPredicate<Optional<ConfigType>, Class<?>> {
        // this is a marker interface extending the appropriately parameterized builtin predicate type
    }

    /**
     * Find candidate OSGi config annotations on the given {@link AnnotatedElement}, returning a stream of only those
     * matching one of the desired config types. An annotation matches a config type only if the annotation's own type
     * is the same as the config type, or if the annotation is a {@link ConfigType} and its
     * {@link ConfigType#type()} is the same as the config type. If the {@link AnnotatedElement} has a
     * {@link ConfigTypes} annotation, its nested {@link ConfigType} annotations will be considered as well.
     *
     * @param element the annotated element
     * @return a stream of annotations
     */
    public static Stream<Annotation> findConfigTypeAnnotations(@NotNull AnnotatedElement element) {
        return findConfigTypeAnnotations(element, null);
    }

    /**
     * Find candidate OSGi config annotations on the given {@link AnnotatedElement}, returning a stream of only those
     * matching one of the desired config types. An annotation matches a config type only if the annotation's own type
     * is the same as the config type, or if the annotation is a {@link ConfigType} and its
     * {@link ConfigType#type()} is the same as the config type. If the {@link AnnotatedElement} has a
     * {@link ConfigTypes} annotation, its nested {@link ConfigType} annotations will be considered as well.
     *
     * @param element             the annotated element
     * @param configTypePredicate an optional subsequent predicate for the applicable configType
     * @return a stream of annotations
     */
    public static Stream<Annotation> findConfigTypeAnnotations(
            @NotNull AnnotatedElement element, @Nullable ConfigTypePredicate configTypePredicate) {
        return Stream.of(element.getAnnotations())
                .flatMap(ConfigAnnotationUtil::flattenAnnotation)
                .filter(ConfigAnnotationUtil.configTypeAnnotationFilter(configTypePredicate));
    }

    /**
     * Find candidate OSGi config annotations in the given collection, returning a stream of only those
     * matching one of the desired config types. An annotation matches a config type only if the annotation's own type
     * is the same as the config type, or if the annotation is a {@link ConfigType} and its
     * {@link ConfigType#type()} is the same as the config type. If the collection has a
     * {@link ConfigTypes} annotation, its nested {@link ConfigType} annotations will be considered as well.
     *
     * @param annotations a collection of annotations
     * @return a stream of annotations
     */
    public static Stream<Annotation> findConfigTypeAnnotations(@NotNull Collection<Annotation> annotations) {
        return findConfigTypeAnnotations(annotations, null);
    }

    /**
     * Find candidate OSGi config annotations in the given collection, returning a stream of only those
     * matching one of the desired config types. An annotation matches a config type only if the annotation's own type
     * is the same as the config type, or if the annotation is a {@link ConfigType} and its
     * {@link ConfigType#type()} is the same as the config type. If the collection has a
     * {@link ConfigTypes} annotation, its nested {@link ConfigType} annotations will be considered as well.
     *
     * @param annotations         a collection of annotations
     * @param configTypePredicate an optional subsequent predicate for the applicable configType
     * @return a stream of annotations
     */
    public static Stream<Annotation> findConfigTypeAnnotations(
            @NotNull Collection<Annotation> annotations, @Nullable ConfigTypePredicate configTypePredicate) {
        return annotations.stream()
                .flatMap(ConfigAnnotationUtil::flattenAnnotation)
                .filter(ConfigAnnotationUtil.configTypeAnnotationFilter(configTypePredicate));
    }

    /**
     * Find {@link SetConfig} annotations on the given {@link AnnotatedElement}. If the {@link AnnotatedElement} has
     * an {@link SetConfigs} annotation, its nested {@link SetConfig} annotations will be included as well.
     *
     * @param element the annotated element
     * @return a stream of annotations
     */
    public static Stream<SetConfig> findUpdateConfigAnnotations(@NotNull AnnotatedElement element) {
        return Stream.of(element.getAnnotations())
                .flatMap(ConfigAnnotationUtil::flattenAnnotation)
                .filter(annotation -> SetConfig.class.isAssignableFrom(annotation.annotationType()))
                .map(SetConfig.class::cast);
    }

    /**
     * Find {@link SetConfig} annotations in the given collection. If the collection has
     * an {@link SetConfigs} annotation, its nested {@link SetConfig} annotations will be included as well.
     *
     * @param annotations a collection of annotations
     * @return a stream of annotations
     */
    public static Stream<SetConfig> findUpdateConfigAnnotations(@NotNull Collection<Annotation> annotations) {
        return annotations.stream()
                .flatMap(ConfigAnnotationUtil::flattenAnnotation)
                .filter(annotation -> SetConfig.class.isAssignableFrom(annotation.annotationType()))
                .map(SetConfig.class::cast);
    }

    /**
     * Utility function for use as a flatMap expression for annotation streams that expands an {@link ConfigTypes}
     * annotation into a substream of {@link ConfigType} annotations, and an {@link SetConfigs} annotation into
     * a substream of {@link SetConfig} annotations.
     *
     * @param annotation input annotation
     * @return the flattened stream of annotations
     */
    private static Stream<Annotation> flattenAnnotation(@NotNull Annotation annotation) {
        if (ConfigTypes.class.isAssignableFrom(annotation.annotationType())) {
            return Stream.of(((ConfigTypes) annotation).value());
        } else if (SetConfigs.class.isAssignableFrom(annotation.annotationType())) {
            return Stream.of(((SetConfigs) annotation).value());
        } else {
            return Stream.of(annotation);
        }
    }

    /**
     * Utility function for filtering out component property types that can't be mapped to configurations.
     *
     * @param configType Config type
     * @return true if the provided class is a valid config type
     */
    public static boolean isValidConfigType(@NotNull Class<?> configType) {
        return (configType.isAnnotation() || configType.isInterface())
                && AbstractConfigTypeReflectionProvider.getInstance(configType).isValidConfigType()
                && EXCLUDE_FEATURE_ANNOTATIONS.stream().noneMatch(excluded -> excluded.isAssignableFrom(configType));
    }

    /**
     * Utility function that returns a predicate for use as a filter expression for
     * {@link #findConfigTypeAnnotations(AnnotatedElement)} and
     * {@link #findConfigTypeAnnotations(Collection)} that reduces the input stream of annotations based on
     * provided set of allowed config types.
     *
     * @param configTypePredicate an optional subsequent predicate for the applicable configType
     * @return an annotation stream predicate
     */
    public static Predicate<Annotation> configTypeAnnotationFilter(
            @Nullable ConfigAnnotationUtil.ConfigTypePredicate configTypePredicate) {
        final ConfigTypePredicate primary = (parent, configType) -> isValidConfigType(configType);
        final ConfigTypePredicate andThen =
                Optional.ofNullable(configTypePredicate).orElse((parentAnnotation, configType) -> true);
        return annotation -> {
            if (ConfigType.class.isAssignableFrom(annotation.annotationType())) {
                final ConfigType parentAnnotation = (ConfigType) annotation;
                return primary.and(andThen).test(Optional.of(parentAnnotation), parentAnnotation.type());
            } else {
                return primary.and(andThen).test(Optional.empty(), annotation.annotationType());
            }
        };
    }

    /**
     * Return the appropriate config type for the given test parameter type, if it supported for binding. If the
     * provided type is an array, the array's component type will be returned if it is a supported type.
     *
     * @param type the candidate parameter type
     * @return an optional containing a supported config type, or empty if not supported.
     */
    public static Optional<Class<?>> determineSupportedConfigType(@NotNull Class<?> type) {
        if (type.isArray()) {
            return determineSupportedConfigType(type.getComponentType());
        } else if (type.isAnnotation() || type.isInterface()) {
            return Optional.of(type);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Returns an array of configs matching the specified parameter config type.
     *
     * @param configCollection the config collection
     * @param configType       the parameter config type class
     * @param <T>              the desired config type
     * @return an array of matching configs
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] resolveParameterToArray(
            @NotNull ConfigCollection configCollection, @NotNull Class<T> configType) {
        return configCollection.configStream(configType).toArray((int size) ->
                (T[]) Array.newInstance(configType, size));
    }

    /**
     * Returns the first {@link TypedConfig} from the {@link ConfigCollection}, if present, after skipping the same
     * number of values as there are matching signature parameter types with an index lower than the current
     * parameterIndex. For example, if we are injecting values into a test method signature that accepts two
     * {@code MyConfig} arguments, this method will be called twice for the same configCollection, parameterConfigType,
     * and signatureParameterTypes array. The first call will specify a parameterIndex of 0, which will return the first
     * value from the collection, and the second call will specify a parameterIndex of 1 which will return the second
     * value from the collection. This method does not consider array or {@link ConfigCollection} parameter types when
     * skipping values.
     *
     * @param configCollection        the config collection
     * @param parameterConfigType     the parameter config type class (must be an interface or an annotation type)
     * @param signatureParameterTypes the types of the signature's parameters
     * @param parameterIndex          the 0-based index of the parameter in the executable's signature
     * @param <T>                     the parameter config type
     * @return a single parameter value if available, or empty
     */
    public static <T> Optional<TypedConfig<T>> resolveParameterToTypedConfig(
            @NotNull ConfigCollection configCollection,
            @NotNull Class<T> parameterConfigType,
            @NotNull Class<?>[] signatureParameterTypes,
            int parameterIndex) {
        if (parameterIndex < 0
                || parameterIndex >= signatureParameterTypes.length
                || !signatureParameterTypes[parameterIndex].isAssignableFrom(parameterConfigType)) {
            // require a valid signature
            return Optional.empty();
        }
        final long skip = Stream.of(signatureParameterTypes)
                .limit(parameterIndex)
                .filter(parameterConfigType::equals)
                .count();
        return configCollection.stream(parameterConfigType).skip(skip).findFirst();
    }

    /**
     * Returns the first config value from the {@link ConfigCollection}, if present, after skipping the same number of
     * values as there are matching signature parameter types with an index lower than the current parameterIndex.
     * For example, if we are injecting values into a test method signature that accepts two {@code MyConfig} arguments,
     * this method will be called twice for the same configCollection, parameterConfigType, and signatureParameterTypes
     * array. The first call will specify a parameterIndex of 0, which will return the first value from the collection,
     * and the second call will specify a parameterIndex of 1 which will return the second value from the collection.
     * This method does not consider array or {@link ConfigCollection} parameter types when skipping values.
     *
     * @param configCollection        the config collection
     * @param parameterConfigType     the parameter config type class (must be an interface or an annotation type)
     * @param signatureParameterTypes the types of the signature's parameters
     * @param parameterIndex          the 0-based index of the parameter in the executable's signature
     * @param <T>                     the parameter config type
     * @return a single parameter value if available, or empty
     */
    public static <T> Optional<T> resolveParameterToValue(
            @NotNull ConfigCollection configCollection,
            @NotNull Class<T> parameterConfigType,
            @NotNull Class<?>[] signatureParameterTypes,
            int parameterIndex) {
        return resolveParameterToTypedConfig(
                        configCollection, parameterConfigType, signatureParameterTypes, parameterIndex)
                .map(TypedConfig::getConfig);
    }

    /**
     * Returns the first config map from the {@link ConfigCollection}, if present, after skipping the same number of
     * values as there are matching signature parameter types with an index lower than the current parameterIndex.
     * For example, if we are injecting values into a test method signature that accepts two {@code MyConfig} arguments,
     * this method will be called twice for the same configCollection, parameterConfigType, and signatureParameterTypes
     * array. The first call will specify a parameterIndex of 0, which will return the first value from the collection,
     * and the second call will specify a parameterIndex of 1 which will return the second value from the collection.
     * This method does not consider array or {@link ConfigCollection} parameter types when skipping values.
     *
     * @param configCollection        the config collection
     * @param parameterConfigType     the parameter config type class (must be an interface or an annotation type)
     * @param signatureParameterTypes the types of the signature's parameters
     * @param parameterIndex          the 0-based index of the parameter in the executable's signature
     * @param <T>                     the parameter config type
     * @return a single parameter value if available, or empty
     */
    public static <T> Optional<Map<String, Object>> resolveParameterToConfigMap(
            @NotNull ConfigCollection configCollection,
            @NotNull Class<T> parameterConfigType,
            @NotNull Class<?>[] signatureParameterTypes,
            int parameterIndex) {
        return resolveParameterToTypedConfig(
                        configCollection, parameterConfigType, signatureParameterTypes, parameterIndex)
                .map(TypedConfig::getConfigMap);
    }
}
