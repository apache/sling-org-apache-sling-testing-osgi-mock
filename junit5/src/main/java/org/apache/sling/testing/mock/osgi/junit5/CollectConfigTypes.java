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
package org.apache.sling.testing.mock.osgi.junit5;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.osgi.service.component.annotations.Component;

/**
 * Annotate a {@link org.apache.sling.testing.mock.osgi.config.annotations.ConfigCollection} test parameter to only
 * include {@link org.apache.sling.testing.mock.osgi.config.annotations.ConfigType}-constructed configs and optionally
 * override the configuration pid applied to them.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface CollectConfigTypes {

    /**
     * Optionally specify a configuration pid to apply to any collected {@link org.apache.sling.testing.mock.osgi.config.annotations.ConfigType}
     * annotations. A non-empty value will override any non-empty {@link org.apache.sling.testing.mock.osgi.config.annotations.ConfigType#pid()}
     * attributes specified by those collected annotations. In order to specify the name of the {@link #component()}
     * class as a configuration PID, set this value to {@link org.osgi.service.component.annotations.Component#NAME}.
     * The default value is the empty string, which skips loading any configuration from ConfigurationAdmin.
     *
     * @return a configuration pid, or an empty string
     */
    String pid() default Component.NAME;

    /**
     * When {@link #pid()} is set to {@link org.osgi.service.component.annotations.Component#NAME}, set this attribute
     * to a class whose name should be used instead.
     *
     * @return the configurable component class
     */
    Class<?> component() default Void.class;
}
