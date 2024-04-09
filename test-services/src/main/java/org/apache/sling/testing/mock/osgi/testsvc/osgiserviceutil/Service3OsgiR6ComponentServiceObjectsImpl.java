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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.FieldOption;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

@Component
public class Service3OsgiR6ComponentServiceObjectsImpl implements Service3OsgiR6 {

    @Reference
    private ComponentServiceObjects<ServiceInterface1> reference1;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ComponentServiceObjects<ServiceInterface1Optional> reference1Optional;

    @Reference(
            cardinality = ReferenceCardinality.AT_LEAST_ONE,
            policy = ReferencePolicy.DYNAMIC,
            policyOption = ReferencePolicyOption.GREEDY)
    private volatile List<ComponentServiceObjects<ServiceInterface2>> references2 = new ArrayList<>();

    @Reference(
            service = ServiceInterface3.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            policyOption = ReferencePolicyOption.GREEDY)
    private volatile List<ComponentServiceObjects<ServiceSuperInterface3>> references3;

    @Reference(
            service = ServiceInterface3.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            target = "(prop1=abc)",
            policy = ReferencePolicy.DYNAMIC,
            policyOption = ReferencePolicyOption.GREEDY)
    private volatile List<ComponentServiceObjects<ServiceSuperInterface3>> references3Filtered;

    @Reference(
            cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.DYNAMIC,
            policyOption = ReferencePolicyOption.GREEDY,
            name = "reference3DynamicFiltered")
    private volatile ComponentServiceObjects<ServiceSuperInterface3> reference3DynamicFiltered;

    @Reference(
            service = ServiceInterface3.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            policyOption = ReferencePolicyOption.GREEDY,
            fieldOption = FieldOption.UPDATE)
    private volatile Set<ComponentServiceObjects<ServiceSuperInterface3>> references3Set;

    @Reference(
            service = ServiceInterface3.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            policyOption = ReferencePolicyOption.GREEDY)
    private volatile Collection<ComponentServiceObjects<ServiceSuperInterface3>> references3Collection;

    private ComponentContext componentContext;
    private Map<String, Object> config;

    @Activate
    private void activate(ComponentContext ctx) {
        this.componentContext = ctx;
        this.config = DictionaryTo.map(ctx.getProperties());
    }

    @Deactivate
    private void deactivate(ComponentContext ctx) {
        this.componentContext = null;
    }

    @Modified
    private void modified(Map<String, Object> newConfig) {
        this.config = newConfig;
    }

    public ServiceInterface1 getReference1() {
        return Optional.ofNullable(this.reference1)
                .map(ComponentServiceObjects::getService)
                .orElse(null);
    }

    public ServiceInterface1Optional getReference1Optional() {
        return Optional.ofNullable(this.reference1Optional)
                .map(ComponentServiceObjects::getService)
                .orElse(null);
    }

    public List<ServiceInterface2> getReferences2() {
        return references2.stream().map(ComponentServiceObjects::getService).collect(Collectors.toList());
    }

    public List<ServiceSuperInterface3> getReferences3() {
        return this.references3.stream()
                .map(ComponentServiceObjects::getService)
                .collect(Collectors.toList());
    }

    public List<ServiceSuperInterface3> getReferences3Filtered() {
        return this.references3Filtered.stream()
                .map(ComponentServiceObjects::getService)
                .collect(Collectors.toList());
    }

    public ServiceSuperInterface3 getReference3DynamicFiltered() {
        return Optional.ofNullable(this.reference3DynamicFiltered)
                .map(ComponentServiceObjects::getService)
                .orElse(null);
    }

    public Set<ServiceSuperInterface3> getReferences3Set() {
        return this.references3Set.stream()
                .map(ComponentServiceObjects::getService)
                .collect(Collectors.toSet());
    }

    public Collection<ServiceSuperInterface3> getReferences3Collection() {
        return this.references3Collection.stream()
                .map(ComponentServiceObjects::getService)
                .collect(Collectors.toList());
    }

    public ComponentContext getComponentContext() {
        return this.componentContext;
    }

    public Map<String, Object> getConfig() {
        return config;
    }
}
