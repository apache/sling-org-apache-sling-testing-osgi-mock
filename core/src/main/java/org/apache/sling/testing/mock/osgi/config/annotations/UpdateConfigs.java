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

/**
 * {@link java.lang.annotation.Repeatable} container annotation for {@link UpdateConfig}. This annotation is used
 * either implicitly or explicitly to specify multiple {@link UpdateConfig} annotations on a single
 * {@link java.lang.reflect.AnnotatedElement}.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface UpdateConfigs {
    /**
     * Return an array of nested {@link UpdateConfig} annotations.
     *
     * @return the array of config updates
     */
    UpdateConfig[] value() default {};
}
