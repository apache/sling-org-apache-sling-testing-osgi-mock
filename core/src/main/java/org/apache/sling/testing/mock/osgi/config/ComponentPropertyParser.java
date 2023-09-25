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

import org.apache.sling.testing.mock.osgi.config.annotations.ApplyConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
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
 * provided via a {@link ApplyConfig} annotation.
 */
public final class ComponentPropertyParser {
    private static final Logger log = LoggerFactory.getLogger(ComponentPropertyParser.class);
    private static final Pattern IDENTIFIERTOPROPERTY = Pattern
            .compile("(__)|(_)|(\\$_\\$)|(\\$\\$)|(\\$)");
    private static final Pattern PROPERTY_PATTERN = Pattern.compile(
            "\\s*(?<key>[^=\\s:]+)\\s*(?::\\s*(?<type>Boolean|Byte|Character|Short|Integer|Long|Float|Double|String)\\s*)?=(?<value>.*)");
    private static final String PROPERTY_PATTERN_CAPTURE_GROUP_VALUE = "value";

    private static final Set<Class<?>> BOXES = Stream.of(
                    Boolean.class, Byte.class, Character.class, Short.class,
                    Integer.class, Long.class, Float.class, Double.class)
            .collect(Collectors.toSet());

    private ComponentPropertyParser() {
        // prevent instantiation
    }

    static String unescape(@NotNull final String escapeSequence) {
        switch (escapeSequence) {
            case "__":
                return "_";
            case "_":
                return ".";
            case "$_$":
                return "-";
            case "$$":
                return "\\$";
            case "$":
                return "";
            default:
                throw new IllegalArgumentException("Unsupported escape sequence " + escapeSequence);
        }
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
            m.appendReplacement(b, unescape(m.group()));
        } while (m.find());
        m.appendTail(b);
        final String propName = b.toString();
        return Optional.ofNullable(prefix)
                .map(pfx -> pfx.concat(propName))
                .orElse(propName);
    }

    @SuppressWarnings("java:S127")
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
                    sb.insert(i, '.');
                    i++; // increment the index because we inserted a character
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

    static boolean isSupportedPropertyMapValueType(Class<?> attributeType) {
        if (attributeType.isArray()) {
            return isSupportedPropertyMapValueType(attributeType.getComponentType());
        }
        return attributeType.isPrimitive() || BOXES.contains(attributeType) || attributeType.equals(String.class);
    }

    static void getAnnotationDefaults(@NotNull final Class<? extends Annotation> annotationType,
                                      @NotNull final Map<String, Object> values) {

        final AbstractPropertyDefaultsProvider defaultsProvider =
                AbstractPropertyDefaultsProvider.getInstance(annotationType);

        Map<String, Object> defaults = defaultsProvider.getDefaults(values);
        if (!defaults.isEmpty()) {
            values.putAll(defaults);
        }
    }

    static Map<String, Object> getTypedProperties(Map<String, String> propertyType, Map<String, List<String>> map) {
        final Map<String, Object> returnProps = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            final String name = entry.getKey();
            final List<String> values = entry.getValue();
            switch (propertyType.getOrDefault(name, "String")) {
                case "Boolean":
                    returnProps.put(name, values.stream()
                            .map(Boolean::parseBoolean).toArray(Boolean[]::new));
                    break;
                case "Byte":
                    returnProps.put(name, values.stream()
                            .map(Byte::parseByte).toArray(Byte[]::new));
                    break;
                case "Character":
                    returnProps.put(name, values.stream()
                            .map(str -> str.charAt(0)).toArray(Character[]::new));
                    break;
                case "Short":
                    returnProps.put(name, values.stream()
                            .map(Short::parseShort).toArray(Short[]::new));
                    break;
                case "Integer":
                    returnProps.put(name, values.stream()
                            .map(Integer::parseInt).toArray(Integer[]::new));
                    break;
                case "Long":
                    returnProps.put(name, values.stream()
                            .map(Long::parseLong).toArray(Long[]::new));
                    break;
                case "Float":
                    returnProps.put(name, values.stream()
                            .map(Float::parseFloat).toArray(Float[]::new));
                    break;
                case "Double":
                    returnProps.put(name, values.stream()
                            .map(Double::parseDouble).toArray(Double[]::new));
                    break;
                case "String":
                default:
                    returnProps.put(name, values.toArray(new String[0]));
                    break;
            }
        }
        return returnProps;
    }

    public static Map<String, Object> parse(@NotNull String[] properties) {
        final Map<String, String> propertyType = new HashMap<>();
        final Map<String, List<String>> map = new HashMap<>();
        final Function<String, List<String>> getValues =
                key -> map.computeIfAbsent(key, ignored -> new LinkedList<>());

        for (String p : properties) {
            Matcher m = PROPERTY_PATTERN.matcher(p);
            if (m.matches()) {
                String key = m.group("key");
                String type = m.group("type");
                if (type != null) {
                    propertyType.put(key, type);
                }
                String value = m.group(PROPERTY_PATTERN_CAPTURE_GROUP_VALUE);
                getValues.apply(key).add(value);
            } else {
                log.warn("Malformed property '{}'", p);
            }
        }

        return getTypedProperties(propertyType, map);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parse(@NotNull Class<?> configType, @NotNull String[] properties) {
        final Map<String, Object> returnProps = parse(properties);

        if (configType.isAnnotation()) {
            getAnnotationDefaults((Class<? extends Annotation>) configType, returnProps);
        }
        return returnProps;
    }
}
