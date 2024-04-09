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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.FieldOption;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

@Component(
        reference = {
            @Reference(
                    name = "reference2",
                    service = ServiceInterface2.class,
                    cardinality = ReferenceCardinality.AT_LEAST_ONE,
                    policy = ReferencePolicy.DYNAMIC,
                    policyOption = ReferencePolicyOption.GREEDY,
                    bind = "bindReference2",
                    unbind = "unbindReference2")
        })
public class Service3 implements ServiceInterface2 {

    @Reference(bind = "bindReference1", unbind = "unbindReference1", policy = ReferencePolicy.DYNAMIC)
    private volatile ServiceInterface1 reference1;

    @Reference(
            cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.DYNAMIC,
            bind = "bindReference1Optional",
            unbind = "unbindReference1Optional")
    private volatile ServiceInterface1Optional reference1Optional;

    private List<ServiceReference<ServiceInterface2>> references2 = new ArrayList<>();

    @Reference(
            name = "reference3",
            service = ServiceInterface3.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            policyOption = ReferencePolicyOption.GREEDY,
            bind = "bindReference3",
            unbind = "unbindReference3")
    private volatile List<ServiceSuperInterface3> references3 = new ArrayList<>();

    private List<Map<String, Object>> reference3Configs = new ArrayList<>();

    @Reference(
            name = "references3Set",
            service = ServiceInterface3.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            policyOption = ReferencePolicyOption.GREEDY,
            fieldOption = FieldOption.UPDATE)
    private volatile Set<ServiceSuperInterface3> references3Set = new HashSet<>();

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
        return this.reference1;
    }

    public ServiceInterface1Optional getReference1Optional() {
        return this.reference1Optional;
    }

    public List<ServiceInterface2> getReferences2() {
        List<ServiceInterface2> services = new ArrayList<>();
        for (ServiceReference<?> serviceReference : references2) {
            services.add((ServiceInterface2) componentContext.getBundleContext().getService(serviceReference));
        }
        return services;
    }

    public List<ServiceSuperInterface3> getReferences3() {
        return this.references3;
    }

    public List<Map<String, Object>> getReference3Configs() {
        return this.reference3Configs;
    }

    public Set<ServiceSuperInterface3> getReferences3Set() {
        return this.references3Set;
    }

    public ComponentContext getComponentContext() {
        return this.componentContext;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    void bindReference1Optional(ServiceInterface1Optional service) {
        reference1Optional = service;
    }

    void unbindReference1Optional(ServiceInterface1Optional service) {
        reference1Optional = null;
    }

    void bindReference1(ServiceInterface1 service) {
        reference1 = service;
    }

    void unbindReference1(ServiceInterface1 service) {
        reference1 = null;
    }

    void bindReference2(ServiceReference<ServiceInterface2> serviceReference) {
        references2.add(serviceReference);
    }

    void unbindReference2(ServiceReference<ServiceInterface2> serviceReference) {
        references2.remove(serviceReference);
    }

    void bindReference3(ServiceSuperInterface3 service, Map<String, Object> serviceConfig) {
        references3.add(service);
        reference3Configs.add(serviceConfig);
    }

    void unbindReference3(ServiceSuperInterface3 service, Map<String, Object> serviceConfig) {
        references3.remove(service);
        reference3Configs.remove(serviceConfig);
    }

    void bindReference3Set(ServiceSuperInterface3 service, Map<String, Object> serviceConfig) {
        references3Set.add(service);
    }

    void unbindReference3Set(ServiceSuperInterface3 service, Map<String, Object> serviceConfig) {
        references3Set.remove(service);
    }
}
