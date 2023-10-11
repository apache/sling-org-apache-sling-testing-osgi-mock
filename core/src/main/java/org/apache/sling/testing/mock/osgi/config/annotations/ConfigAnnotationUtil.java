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
package org.apache.sling.testing.mock.osgi.config.annotations;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Common functions for resolving OSGi config test parameters.
 */
public final class ConfigAnnotationUtil {
    private static final Set<Class<? extends Annotation>> EXCLUDE_FEATURE_ANNOTATIONS = Set.of(
            ApplyConfigs.class, ApplyConfig.class, UpdateConfigs.class, UpdateConfig.class);

    private ConfigAnnotationUtil() {
        // prevent instantiation
    }

    /**
     * Find candidate OSGi config annotations on the given {@link AnnotatedElement}, returning a stream of only those
     * matching one of the desired config types. An annotation matches a config type only if the annotation's own type
     * is the same as the config type, or if the annotation is a {@link ApplyConfig} and its
     * {@link ApplyConfig#type()} is the same as the config type. If the {@link AnnotatedElement} has a
     * {@link ApplyConfigs} annotation, its nested {@link ApplyConfig} annotations will be considered as well.
     *
     * @param element     the annotated element
     * @param configTypes the desired config types
     * @return a stream of annotations
     */
    public static Stream<Annotation> findApplicableConfigAnnotations(@NotNull AnnotatedElement element,
                                                                     @NotNull Set<Class<?>> configTypes) {
        return Stream.of(element.getAnnotations())
                .flatMap(ConfigAnnotationUtil::flattenAnnotation)
                .filter(ConfigAnnotationUtil.applyConfigAnnotationPredicate(configTypes));
    }

    /**
     * Find candidate OSGi config annotations in the given collection, returning a stream of only those
     * matching one of the desired config types. An annotation matches a config type only if the annotation's own type
     * is the same as the config type, or if the annotation is a {@link ApplyConfig} and its
     * {@link ApplyConfig#type()} is the same as the config type. If the collection has a
     * {@link ApplyConfigs} annotation, its nested {@link ApplyConfig} annotations will be considered as well.
     *
     * @param annotations a collection of annotations
     * @param configTypes the desired config types
     * @return a stream of annotations
     */
    public static Stream<Annotation> findApplicableConfigAnnotations(@NotNull Collection<Annotation> annotations,
                                                                     @NotNull Set<Class<?>> configTypes) {
        return annotations.stream()
                .flatMap(ConfigAnnotationUtil::flattenAnnotation)
                .filter(ConfigAnnotationUtil.applyConfigAnnotationPredicate(configTypes));
    }

    /**
     * Find {@link UpdateConfig} annotations on the given {@link AnnotatedElement}. If the {@link AnnotatedElement} has
     * an {@link UpdateConfigs} annotation, its nested {@link UpdateConfig} annotations will be included as well.
     *
     * @param element the annotated element
     * @return a stream of annotations
     */
    public static Stream<UpdateConfig> findUpdateConfigAnnotations(@NotNull AnnotatedElement element) {
        return Stream.of(element.getAnnotations())
                .flatMap(ConfigAnnotationUtil::flattenAnnotation)
                .filter(annotation -> UpdateConfig.class.isAssignableFrom(annotation.annotationType()))
                .map(UpdateConfig.class::cast);
    }

    /**
     * Find {@link UpdateConfig} annotations in the given collection. If the collection has
     * an {@link UpdateConfigs} annotation, its nested {@link UpdateConfig} annotations will be included as well.
     *
     * @param annotations a collection of annotations
     * @return a stream of annotations
     */
    public static Stream<UpdateConfig> findUpdateConfigAnnotations(@NotNull Collection<Annotation> annotations) {
        return annotations.stream()
                .flatMap(ConfigAnnotationUtil::flattenAnnotation)
                .filter(annotation -> UpdateConfig.class.isAssignableFrom(annotation.annotationType()))
                .map(UpdateConfig.class::cast);
    }

    /**
     * Utility function for use as a flatMap expression for annotation streams that expands an {@link ApplyConfigs}
     * annotation into a substream of {@link ApplyConfig} annotations, and an {@link UpdateConfigs} annotation into
     * a substream of {@link UpdateConfig} annotations.
     *
     * @param annotation input annotation
     * @return the flattened stream of annotations
     */
    private static Stream<Annotation> flattenAnnotation(@NotNull Annotation annotation) {
        if (ApplyConfigs.class.isAssignableFrom(annotation.annotationType())) {
            return Stream.of(((ApplyConfigs) annotation).value());
        } else if (UpdateConfigs.class.isAssignableFrom(annotation.annotationType())) {
            return Stream.of(((UpdateConfigs) annotation).value());
        } else {
            return Stream.of(annotation);
        }
    }

    /**
     * Utility function that returns a predicate for use as a filter expression for
     * {@link #findApplicableConfigAnnotations(AnnotatedElement, Set)} and
     * {@link #findApplicableConfigAnnotations(Collection, Set)} that reduces the input stream of annotations based on
     * provided set of allowed config types.
     *
     * @param configTypes the allowed config types
     * @return an annotation stream predicate
     */
    private static Predicate<Annotation> applyConfigAnnotationPredicate(@NotNull Set<Class<?>> configTypes) {
        return annotation -> {
            if (ApplyConfig.class.isAssignableFrom(annotation.annotationType())) {
                final Class<?> configType = ((ApplyConfig) annotation).type();
                return configTypes.contains(configType)
                        && EXCLUDE_FEATURE_ANNOTATIONS.stream().noneMatch(excluded ->
                        excluded.isAssignableFrom(configType));
            } else {
                return configTypes.contains(annotation.annotationType())
                        && EXCLUDE_FEATURE_ANNOTATIONS.stream().noneMatch(excluded ->
                        excluded.isAssignableFrom(annotation.annotationType()));
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
    public static <T> T[] resolveParameterToArray(@NotNull ConfigCollection configCollection,
                                                  @NotNull Class<T> configType) {
        return configCollection.configStream(configType)
                .toArray((int size) -> (T[]) Array.newInstance(configType, size));
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
    public static <T> Optional<T> resolveParameterToValue(@NotNull ConfigCollection configCollection,
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
        return configCollection.configStream(parameterConfigType).skip(skip).findFirst();
    }

}
