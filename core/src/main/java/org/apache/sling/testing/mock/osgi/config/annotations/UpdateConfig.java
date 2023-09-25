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

import org.osgi.service.component.annotations.Component;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Define this annotation on a test class or method to use the {@link org.osgi.service.cm.ConfigurationAdmin} service
 * to update the persisted properties for the configuration whose pid matches the {@link #value()} attribute.
 * Updates should be applied top-down for each test context scope, from with the outermost (class-level) to the
 * innermost (method-level).
 */
@Repeatable(UpdateConfigs.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface UpdateConfig {

    /**
     * Specify a configuration pid to update with values specified by {@link #property()}.
     *
     * @return a configuration pid
     */
    String value();

    /**
     * Parsed like {@link Component#property()}.
     *
     * @return osgi config properties
     */
    String[] property() default {};
}
