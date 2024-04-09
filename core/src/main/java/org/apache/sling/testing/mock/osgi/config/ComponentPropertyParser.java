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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A few items borrowed from biz.aQute.bndlib DSAnnotationReader and PropertyDef to construct a Map for
 * felix scr Annotations.toObject() from a combination of an annotation type and an array of property key=value strings
 * provided via a {@link org.apache.sling.testing.mock.osgi.config.annotations.ConfigType} annotation.
 */
public final class ComponentPropertyParser {
    private static final Logger log = LoggerFactory.getLogger(ComponentPropertyParser.class);
    private static final Pattern IDENTIFIERTOPROPERTY = Pattern.compile("(__)|(_)|(\\$_\\$)|(\\$\\$)|(\\$)");
    private static final Pattern PROPERTY_PATTERN = Pattern.compile(
            "\\s*(?<key>[^=\\s:]+)\\s*(?::\\s*(?<type>Boolean|Byte|Character|Short|Integer|Long|Float|Double|String)\\s*)?=(?<value>.*)");
    private static final String PROPERTY_PATTERN_CAPTURE_GROUP_VALUE = "value";

    private static final Set<Class<?>> BOXES = Stream.of(
                    Boolean.class,
                    Byte.class,
                    Character.class,
                    Short.class,
                    Integer.class,
                    Long.class,
                    Float.class,
                    Double.class)
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

    @SuppressWarnings("null")
    static String identifierToPropertyName(@NotNull String name, @Nullable String prefix) {
        Matcher m = IDENTIFIERTOPROPERTY.matcher(name);
        if (!m.find()) {
            return Optional.ofNullable(prefix).map(pfx -> pfx.concat(name)).orElse(name);
        }
        StringBuffer b = new StringBuffer();
        do {
            m.appendReplacement(b, unescape(m.group()));
        } while (m.find());
        m.appendTail(b);
        final String propName = b.toString();
        return Optional.ofNullable(prefix).map(pfx -> pfx.concat(propName)).orElse(propName);
    }

    @SuppressWarnings("java:S127")
    static String singleElementAnnotationKey(@NotNull final String simpleName, @Nullable final String prefix) {
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

    static boolean isSupportedNonArraySimpleType(Class<?> attributeType) {
        return attributeType.isPrimitive() || BOXES.contains(attributeType) || attributeType.equals(String.class);
    }

    public static boolean isSupportedPropertyMapValueType(Class<?> attributeType) {
        if (attributeType.isArray()) {
            return isSupportedNonArraySimpleType(attributeType.getComponentType());
        }
        return isSupportedNonArraySimpleType(attributeType);
    }

    public static boolean isSupportedConfigTypeValueType(Class<?> attributeType) {
        if (attributeType.isArray()) {
            return isSupportedConfigTypeValueType(attributeType.getComponentType());
        }
        return isSupportedNonArraySimpleType(attributeType)
                || Class.class.equals(attributeType)
                || attributeType.isEnum();
    }

    static void getDefaults(@NotNull final Class<?> configType, @NotNull final Map<String, Object> values) {

        final AbstractConfigTypeReflectionProvider defaultsProvider =
                AbstractConfigTypeReflectionProvider.getInstance(configType);

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
                    putSingleOrMany(returnProps, name, values, Boolean::parseBoolean, Boolean[]::new);
                    break;
                case "Byte":
                    putSingleOrMany(returnProps, name, values, Byte::parseByte, Byte[]::new);
                    break;
                case "Character":
                    putSingleOrMany(returnProps, name, values, str -> str.charAt(0), Character[]::new);
                    break;
                case "Short":
                    putSingleOrMany(returnProps, name, values, Short::parseShort, Short[]::new);
                    break;
                case "Integer":
                    putSingleOrMany(returnProps, name, values, Integer::parseInt, Integer[]::new);
                    break;
                case "Long":
                    putSingleOrMany(returnProps, name, values, Long::parseLong, Long[]::new);
                    break;
                case "Float":
                    putSingleOrMany(returnProps, name, values, Float::parseFloat, Float[]::new);
                    break;
                case "Double":
                    putSingleOrMany(returnProps, name, values, Double::parseDouble, Double[]::new);
                    break;
                case "String":
                default:
                    putSingleOrMany(returnProps, name, values, Function.identity(), String[]::new);
                    break;
            }
        }
        return returnProps;
    }

    static <T> void putSingleOrMany(
            Map<String, Object> map,
            String key,
            List<String> values,
            Function<? super String, ? extends T> mapper,
            IntFunction<T[]> generator) {
        if (values.size() == 1) {
            map.put(key, mapper.apply(values.get(0)));
        } else if (values.size() > 1) {
            map.put(key, values.stream().map(mapper).toArray(generator));
        }
    }

    public static Map<String, Object> parse(@NotNull String[] properties) {
        final Map<String, String> propertyType = new HashMap<>();
        final Map<String, List<String>> map = new HashMap<>();
        final Function<String, List<String>> getValues = key -> map.computeIfAbsent(key, ignored -> new LinkedList<>());

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

    public static Map<String, Object> parse(@NotNull Class<?> configType, @NotNull String[] properties) {
        final Map<String, Object> returnProps = parse(properties);
        getDefaults(configType, returnProps);
        return returnProps;
    }

    public static void assertOneToOneMapping(@NotNull Class<?> configType, @NotNull String[] properties) {
        final Map<String, Object> props = parse(properties);

        final AbstractConfigTypeReflectionProvider defaultsProvider =
                AbstractConfigTypeReflectionProvider.getInstance(configType);

        Set<String> parsedProperties = new HashSet<>(props.keySet());
        Set<String> expectedProperties = Arrays.stream(defaultsProvider.getMethods())
                .map(defaultsProvider::getPropertyName)
                .collect(Collectors.toSet());
        Set<String> defaults = defaultsProvider.getDefaults(props).keySet();

        List<String> missingExpected = new ArrayList<>(expectedProperties);
        missingExpected.removeAll(parsedProperties);
        missingExpected.removeAll(defaults);

        List<String> unexpectedParsed = new ArrayList<>(parsedProperties);
        unexpectedParsed.removeAll(expectedProperties);

        if (!missingExpected.isEmpty() || !unexpectedParsed.isEmpty()) {
            throw new ConfigTypeStrictnessViolation(String.format(
                    "Config type %s failed one-to-one mapping test (missing=%s unexpected=%s) with properties %s",
                    configType, missingExpected, unexpectedParsed, parsedProperties));
        }
    }
}
