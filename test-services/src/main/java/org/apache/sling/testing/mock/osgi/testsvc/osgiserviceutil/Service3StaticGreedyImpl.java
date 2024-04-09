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
import java.util.List;
import java.util.Map;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

@Component(
        service = Service3StaticGreedy.class,
        reference = {
            @Reference(
                    name = "reference2",
                    service = ServiceInterface2.class,
                    cardinality = ReferenceCardinality.AT_LEAST_ONE,
                    policy = ReferencePolicy.STATIC,
                    policyOption = ReferencePolicyOption.GREEDY,
                    bind = "bindReference2",
                    unbind = "unbindReference2")
        })
public class Service3StaticGreedyImpl implements Service3StaticGreedy {

    @Reference(
            bind = "bindReference1",
            unbind = "unbindReference1",
            policy = ReferencePolicy.STATIC,
            policyOption = ReferencePolicyOption.GREEDY)
    private ServiceInterface1 reference1;

    @Reference(
            cardinality = ReferenceCardinality.OPTIONAL,
            bind = "bindReference1Optional",
            unbind = "unbindReference1Optional",
            policy = ReferencePolicy.STATIC,
            policyOption = ReferencePolicyOption.GREEDY)
    private ServiceInterface1Optional reference1Optional;

    private List<ServiceReference<ServiceInterface2>> references2 = new ArrayList<>();

    @Reference(
            name = "reference3",
            service = ServiceInterface3.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.STATIC,
            policyOption = ReferencePolicyOption.GREEDY,
            bind = "bindReference3",
            unbind = "unbindReference3")
    private List<ServiceSuperInterface3> references3 = new ArrayList<>();

    private List<Map<String, Object>> reference3Configs = new ArrayList<>();

    @Reference(
            service = ServiceInterface3.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            target = "(prop1=abc)",
            policy = ReferencePolicy.STATIC,
            policyOption = ReferencePolicyOption.GREEDY)
    private List<ServiceSuperInterface3> references3Filtered;

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

    @Override
    public ServiceInterface1 getReference1() {
        return this.reference1;
    }

    @Override
    public ServiceInterface1Optional getReference1Optional() {
        return this.reference1Optional;
    }

    @Override
    public List<ServiceInterface2> getReferences2() {
        List<ServiceInterface2> services = new ArrayList<>();
        for (ServiceReference<?> serviceReference : references2) {
            services.add((ServiceInterface2) componentContext.getBundleContext().getService(serviceReference));
        }
        return services;
    }

    @Override
    public List<ServiceSuperInterface3> getReferences3() {
        return this.references3;
    }

    @Override
    public List<Map<String, Object>> getReference3Configs() {
        return this.reference3Configs;
    }

    @Override
    public List<ServiceSuperInterface3> getReferences3Filtered() {
        return this.references3Filtered;
    }

    public ComponentContext getComponentContext() {
        return this.componentContext;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    protected void bindReference1Optional(ServiceInterface1Optional service) {
        reference1Optional = service;
    }

    protected void unbindReference1Optional(ServiceInterface1Optional service) {
        reference1Optional = null;
    }

    protected void bindReference1(ServiceInterface1 service) {
        reference1 = service;
    }

    protected void unbindReference1(ServiceInterface1 service) {
        reference1 = null;
    }

    protected void bindReference2(ServiceReference<ServiceInterface2> serviceReference) {
        references2.add(serviceReference);
    }

    protected void unbindReference2(ServiceReference<ServiceInterface2> serviceReference) {
        references2.remove(serviceReference);
    }

    protected void bindReference3(ServiceSuperInterface3 service, Map<String, Object> serviceConfig) {
        references3.add(service);
        reference3Configs.add(serviceConfig);
    }

    protected void unbindReference3(ServiceSuperInterface3 service, Map<String, Object> serviceConfig) {
        references3.remove(service);
        reference3Configs.remove(serviceConfig);
    }
}
