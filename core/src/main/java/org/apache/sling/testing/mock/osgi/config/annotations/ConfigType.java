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

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.osgi.service.component.annotations.Component;

/**
 * Defines an instance of an OSGi R7 Component Property Type as a combination of a {@link Class} and an array of strings
 * defining property values in the form expected by {@link org.osgi.service.component.annotations.Component#property()}.
 * This provides both runtime retention for OSGi config annotations that do not have {@link RetentionPolicy#RUNTIME},
 * allowing for simple construction through reflection for explicit passing to SCR component constructors and lifecycle
 * methods, as well as repeatability to support defining sequenced, heterogeneous lists of desired types on any single
 * {@link java.lang.reflect.AnnotatedElement}.
 *
 * @see <a href="https://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.component.html#service.component-component.property.types">Component Property Types</a>
 */
@Repeatable(ConfigTypes.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigType {

    /**
     * Required type to construct. This can be an annotation or an interface.
     *
     * @return the type to construct
     */
    Class<?> type();

    /**
     * Specify a configuration pid to load, which will override matching values specified by {@link #property()}. The
     * default value is {@link Component#NAME}, which is a special string ("$") that can be used to specify the name of
     * the {@link #component()} class as a configuration PID.
     *
     * @return a configuration pid, or an empty string
     */
    String pid() default Component.NAME;

    /**
     * When {@link #pid()} is set to {@link org.osgi.service.component.annotations.Component#NAME}, set this attribute
     * to a class whose name should be used instead. The default value is {@link Void}, which has a special significance
     * for this annotation indicating that no configuration should be loaded from ConfigurationAdmin.
     *
     * @return the configurable component class
     */
    Class<?> component() default Void.class;

    /**
     * Treat like {@link org.osgi.service.component.annotations.Component#property()}.
     *
     * @return osgi component properties
     */
    String[] property() default {};

    /**
     * When set to false, throw a {@link org.apache.sling.testing.mock.osgi.config.ConfigTypeStrictnessViolation} on
     * construction if there is not an exact one-to-one mapping between property names specified in {@link #property()}
     * and the addressable attributes of {@link #type()}. Properties loaded from configuration are not considered by
     * the strictness check.
     *
     * @return false to enforce strictness, true to skip the check
     */
    boolean lenient() default false;
}
