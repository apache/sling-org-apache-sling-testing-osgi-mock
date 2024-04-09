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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

@Component(service = Service3StaticGreedy.class)
public class Service3StaticGreedyConstructorInjectionComponentServiceObjectsImpl implements Service3StaticGreedy {

    private final ComponentServiceObjects<ServiceInterface1> reference1;
    private final ComponentServiceObjects<ServiceInterface1Optional> reference1Optional;
    private final List<ComponentServiceObjects<ServiceInterface2>> references2;
    private final List<ComponentServiceObjects<ServiceSuperInterface3>> references3;

    private final ComponentContext componentContext;
    private final Map<String, Object> config;

    // this constructor should be ignored as it contains additional parameters not valid for injection
    @SuppressWarnings("java:S1172") // unused parameter by intention
    public Service3StaticGreedyConstructorInjectionComponentServiceObjectsImpl(
            ComponentServiceObjects<ServiceInterface1> reference1,
            ComponentServiceObjects<ServiceInterface1Optional> reference1Optional,
            List<ComponentServiceObjects<ServiceInterface2>> references2,
            List<ComponentServiceObjects<ServiceSuperInterface3>> references3,
            ComponentContext ctx,
            Map<String, Object> config,
            Object illegalParameter) {

        this.componentContext = ctx;

        this.reference1 = reference1;
        this.reference1Optional = reference1Optional;
        this.references2 = references2;
        this.references3 = references3;

        this.config = config;
    }

    @Activate
    public Service3StaticGreedyConstructorInjectionComponentServiceObjectsImpl(
            @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY)
                    ComponentServiceObjects<ServiceInterface1> reference1,
            @Reference(
                            cardinality = ReferenceCardinality.OPTIONAL,
                            policy = ReferencePolicy.STATIC,
                            policyOption = ReferencePolicyOption.GREEDY)
                    ComponentServiceObjects<ServiceInterface1Optional> reference1Optional,
            @Reference(
                            cardinality = ReferenceCardinality.AT_LEAST_ONE,
                            policy = ReferencePolicy.STATIC,
                            policyOption = ReferencePolicyOption.GREEDY)
                    List<ComponentServiceObjects<ServiceInterface2>> references2,
            @Reference(
                            name = "reference3",
                            service = ServiceInterface3.class,
                            cardinality = ReferenceCardinality.MULTIPLE,
                            policy = ReferencePolicy.STATIC,
                            policyOption = ReferencePolicyOption.GREEDY)
                    List<ComponentServiceObjects<ServiceSuperInterface3>> references3,
            ComponentContext ctx,
            Map<String, Object> config) {

        this.componentContext = ctx;

        this.reference1 = reference1;
        this.reference1Optional = reference1Optional;
        this.references2 = references2;
        this.references3 = references3;

        this.config = config;
    }

    @Override
    public ServiceInterface1 getReference1() {
        return Optional.ofNullable(this.reference1)
                .map(ComponentServiceObjects::getService)
                .orElse(null);
    }

    @Override
    public ServiceInterface1Optional getReference1Optional() {
        return Optional.ofNullable(this.reference1Optional)
                .map(ComponentServiceObjects::getService)
                .orElse(null);
    }

    @Override
    public List<ServiceInterface2> getReferences2() {
        return references2.stream().map(ComponentServiceObjects::getService).collect(Collectors.toList());
    }

    @Override
    public List<ServiceSuperInterface3> getReferences3() {
        return this.references3.stream()
                .map(ComponentServiceObjects::getService)
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getReference3Configs() {
        return this.references3.stream()
                .map(ComponentServiceObjects::getServiceReference)
                .map(ServiceReference::getProperties)
                .map(DictionaryTo::map)
                .collect(Collectors.toList());
    }

    @Override
    public List<ServiceSuperInterface3> getReferences3Filtered() {
        return null;
    }

    public ComponentContext getComponentContext() {
        return this.componentContext;
    }

    public Map<String, Object> getConfig() {
        return config;
    }
}
