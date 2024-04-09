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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.osgi.service.component.annotations.Component;

/**
 * Declares a component configuration pid for which all non-configured component property type annotations in scope
 * are converted to maps and merged to ConfigurationAdmin just prior to test execution. This annotation may only be
 * specified once on any given {@link java.lang.reflect.AnnotatedElement}, and an instance of this annotation on a test
 * method will override an instance of this annotation on the parent class.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoConfig {

    /**
     * The component class to be configured using its name as the configuration pid.
     *
     * @return the component class
     */
    Class<?> value();

    /**
     * If the component pid is different from the provided {@link #value()}, set this attribute appropriately.
     *
     * @return the configuration pid if not the same as the component name
     */
    String pid() default Component.NAME;
}
