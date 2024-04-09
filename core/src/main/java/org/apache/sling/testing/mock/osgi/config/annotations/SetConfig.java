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
 * Define this annotation on a test class or method to use the {@link org.osgi.service.cm.ConfigurationAdmin} service
 * to update the persisted properties for the configuration whose pid matches the {@link #pid()} attribute.
 * Updates should be applied top-down for each test context scope, from with the outermost (class-level) to the
 * innermost (method-level).
 */
@Repeatable(SetConfigs.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface SetConfig {

    /**
     * Specify a configuration pid to update with values specified by {@link #property()}. The default value is
     * {@link Component#NAME}, which is a special string ("$") that can be used to specify the name of the
     * {@link #component()} class as a configuration PID.
     *
     * @return a configuration pid
     */
    String pid() default Component.NAME;

    /**
     * When {@link #pid()} is set to the default value of {@link Component#NAME}, set this attribute to a class whose
     * name should be used instead. This can be more convenient when using {@link SetConfig} in combination with
     * {@link org.apache.sling.testing.mock.osgi.context.OsgiContextImpl#registerInjectActivateService(Class)}. The
     * default value is {@link Void}, which seems perfectly fine for a pid, but which in practice is a  somewhat
     * challenging class to construct for binding the configuration to.
     *
     * @return the configurable component class
     */
    Class<?> component() default Void.class;

    /**
     * Parsed like {@link Component#property()}.
     *
     * @return osgi config properties
     */
    String[] property() default {};
}
