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

import org.apache.sling.testing.mock.osgi.config.annotations.ConfigCollection;
import org.apache.sling.testing.mock.osgi.config.annotations.DynamicConfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate a {@link ConfigCollection} test parameter to specify the config types to collect within the given parameter
 * context.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
public @interface CollectConfigTypes {

    /**
     * Specify the config types to collect. An empty value will result in an empty {@link ConfigCollection}.
     *
     * @return the config types to collect
     */
    Class<?>[] value();

    /**
     * Optionally specify a configuration pid to apply to any collected {@link DynamicConfig} annotations.
     * A non-empty value will override any non-empty {@link DynamicConfig#applyPid()} attributes specified by those
     * collected annotations.
     *
     * @return a configuration pid, or an empty string
     */
    String applyPid() default "";
}
