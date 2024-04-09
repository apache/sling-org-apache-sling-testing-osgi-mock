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
package org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil;

import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class Service3OsgiR8OptionalFieldImpl implements Service3OsgiR8OptionalField {

    @Reference
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    Optional<ServiceInterface1> optionalService1;

    @Reference
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    volatile Optional<ServiceInterface5> optionalService5;

    @Override
    public Optional<ServiceInterface1> getOptionalService1() {
        return optionalService1;
    }

    @Override
    public Optional<ServiceInterface5> getOptionalService5() {
        return optionalService5;
    }
}
