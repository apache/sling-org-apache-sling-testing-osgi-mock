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

import org.apache.sling.testing.mock.osgi.config.annotations.DynamicConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.felix.scr.impl.inject.Annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A few items borrowed from biz.aQute.bndlib DSAnnotationReader and PropertyDef to construct a Map for
 * felix scr Annotations.toObject() from a combination of an annotation type and an array of property key=value strings
 * provided via a {@link DynamicConfig} annotation.
 */
public final class ComponentPropertyParser {
    private static final Logger log = LoggerFactory.getLogger(ComponentPropertyParser.class);
    private static final Pattern IDENTIFIERTOPROPERTY = Pattern
            .compile("(__)|(_)|(\\$_\\$)|(\\$\\$)|(\\$)");
    private static final Pattern PROPERTY_PATTERN = Pattern.compile(
            "\\s*(?<key>[^=\\s:]+)\\s*(?::\\s*(?<type>Boolean|Byte|Character|Short|Integer|Long|Float|Double|String)\\s*)?=(?<value>.*)");

    private static final Set<Class<?>> BOXES = Stream.of(
                    Boolean.class, Byte.class, Character.class, Short.class,
                    Integer.class, Long.class, Float.class, Double.class)
            .collect(Collectors.toSet());

    private ComponentPropertyParser() {
        // prevent instantiation
    }

    static String identifierToPropertyName(@NotNull String name, @Nullable String prefix) {
        Matcher m = IDENTIFIERTOPROPERTY.matcher(name);
        if (!m.find()) {
            return Optional.ofNullable(prefix)
                    .map(pfx -> pfx.concat(name))
                    .orElse(name);
        }
        StringBuffer b = new StringBuffer();
        do {
            switch (m.group()) {
                case "__": // __ to _
                    m.appendReplacement(b, "_");
                    break;
                case "_": // _ to .
                    m.appendReplacement(b, ".");
                    break;
                case "$_$": // $_$ to -
                    m.appendReplacement(b, "-");
                    break;
                case "$$": // $$ to $
                    m.appendReplacement(b, "\\$");
                    break;
                case "$": // $ removed
                    m.appendReplacement(b, "");
                    break;
                // default: // not possible with "(__)|(_)|(\$_\$)|(\$\$)|(\$)"
            }
        } while (m.find());
        m.appendTail(b);
        final String propName = b.toString();
        return Optional.ofNullable(prefix)
                .map(pfx -> pfx.concat(propName))
                .orElse(propName);
    }

    static String singleElementAnnotationKey(@NotNull final String simpleName,
                                             @Nullable final String prefix) {
        int dollar = simpleName.lastIndexOf('$');
        StringBuilder sb = new StringBuilder(dollar <= 0 ? simpleName : simpleName.substring(dollar + 1));
        boolean lastLowerCase = false;
        for (int i = 0; i < sb.length(); i++) {
            char c = sb.charAt(i);
            if (Character.isUpperCase(c)) {
                sb.setCharAt(i, Character.toLowerCase(c));
                if (lastLowerCase) {
                    sb.insert(i++, '.');
                }
                lastLowerCase = false;
            } else {
                lastLowerCase = Character.isLowerCase(c);
            }
        }
        if (prefix != null) {
            return prefix.concat(sb.toString());
        } else {
            return sb.toString();
        }
    }

    static void getAnnotationDefaults(@NotNull final Class<? extends Annotation> annotationType,
                                      @NotNull final Map<String, Object> values) {
        final String prefix = Annotations.getPrefix(annotationType);
        final boolean isSingleElementAnnotation = Annotations.isSingleElementAnnotation(annotationType);
        Map<String, Object> defaults = new HashMap<>();
        for (Method method : Stream.of(annotationType.getMethods())
                .filter(method -> !isSingleElementAnnotation || "value".equals(method.getName()))
                .toArray(Method[]::new)) {
            final Object value = method.getDefaultValue();
            if (value != null) {
                final Class<?> valueType = value.getClass();
                final Class<?> singleType = valueType.isArray() ? valueType.getComponentType() : valueType;
                if (Annotation.class.isAssignableFrom(singleType)) {
                    // check type, exit method with warning if annotation
                    // or annotation array
                    log.warn("Nested annotation type found in member {}, {}",
                            annotationType.getName(), singleType.getName());
                    return;
                }

                // determine property name to set the default value for
                final String propertyName;
                if (isSingleElementAnnotation && "value".equals(method.getName())) {
                    propertyName = singleElementAnnotationKey(annotationType.getSimpleName(), prefix);
                } else {
                    propertyName = identifierToPropertyName(method.getName(), prefix);
                }

                if (values.containsKey(propertyName)) {
                    // skip this default value if the property is already set
                    continue;
                }
                if (singleType.isPrimitive() || BOXES.contains(singleType) || singleType.equals(String.class)) {
                    if (valueType.isArray()) {
                        defaults.put(propertyName, value);
                    } else {
                        Object array = Array.newInstance(singleType, 1);
                        Array.set(array, 0, value);
                        defaults.put(propertyName, array);
                    }
                } else if (singleType.equals(Class.class)) {
                    // Class.class is the same as Class<Class<?>>
                    // this must be transformed to a String representing the FQDN
                    if (valueType.isArray()) {
                        defaults.put(propertyName,
                                Stream.of((Class<?>[]) value).map(Class::getName).toArray(String[]::new));
                    } else {
                        defaults.put(propertyName, new String[]{((Class<?>) value).getName()});
                    }
                }
            }
        }
        values.putAll(defaults);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parse(@NotNull Class<?> configType, @NotNull String[] properties) {
        final Map<String, Object> returnProps = new HashMap<>();
        if (properties.length > 0) {
            Map<String, String> propertyType = new HashMap<>();
            final Map<String, List<String>> map = new HashMap<>();
            final Function<String, List<String>> getValues =
                    (key) -> map.computeIfAbsent(key, (k) -> new LinkedList<>());
            for (String p : properties) {
                Matcher m = PROPERTY_PATTERN.matcher(p);
                if (m.matches()) {
                    String key = m.group("key");
                    String type = m.group("type");
                    if (type != null && propertyType.containsKey(key) && !type.equals(propertyType.get(key))) {
                        log.warn("Inconsistent types for property '{}' on component: {}", p, configType.getName());
                    }
                    if (type != null) {
                        propertyType.put(key, type);
                    }
                    String value = m.group("value");
                    getValues.apply(key).add(value);
                } else {
                    log.warn("Malformed property '{}' on component: {}", p, configType.getName());
                }
            }

            for (String name : map.keySet()) {
                switch (propertyType.getOrDefault(name, "String")) {
                    case "Boolean":
                        returnProps.put(name, map.get(name).stream()
                                .map(Boolean::parseBoolean).toArray(Boolean[]::new));
                        break;
                    case "Byte":
                        returnProps.put(name, map.get(name).stream()
                                .map(Byte::parseByte).toArray(Byte[]::new));
                        break;
                    case "Character":
                        returnProps.put(name, map.get(name).stream()
                                .map(str -> str.charAt(0)).toArray(Character[]::new));
                        break;
                    case "Short":
                        returnProps.put(name, map.get(name).stream()
                                .map(Short::parseShort).toArray(Short[]::new));
                        break;
                    case "Integer":
                        returnProps.put(name, map.get(name).stream()
                                .map(Integer::parseInt).toArray(Integer[]::new));
                        break;
                    case "Long":
                        returnProps.put(name, map.get(name).stream()
                                .map(Long::parseLong).toArray(Long[]::new));
                        break;
                    case "Float":
                        returnProps.put(name, map.get(name).stream()
                                .map(Float::parseFloat).toArray(Float[]::new));
                        break;
                    case "Double":
                        returnProps.put(name, map.get(name).stream()
                                .map(Double::parseDouble).toArray(Double[]::new));
                        break;
                    case "String":
                    default:
                        returnProps.put(name, map.get(name).toArray(new String[0]));
                        break;
                }
            }
        }
        if (configType.isAnnotation()) {
            getAnnotationDefaults((Class<? extends Annotation>) configType, returnProps);
        }
        return returnProps;
    }
}
