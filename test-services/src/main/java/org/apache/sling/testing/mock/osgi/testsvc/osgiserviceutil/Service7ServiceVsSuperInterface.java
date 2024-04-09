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

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

@Component(
        reference = {
            @Reference(
                    name = "interfaceBindList",
                    bind = "bindInterface",
                    unbind = "unbindInterface",
                    service = ServiceInterface3.class,
                    cardinality = ReferenceCardinality.MULTIPLE,
                    policy = ReferencePolicy.DYNAMIC,
                    policyOption = ReferencePolicyOption.GREEDY),
            @Reference(
                    name = "superInterfaceBindList",
                    bind = "bindSuperInterface",
                    unbind = "unbindSuperInterface",
                    service = ServiceSuperInterface3.class,
                    cardinality = ReferenceCardinality.MULTIPLE,
                    policy = ReferencePolicy.DYNAMIC,
                    policyOption = ReferencePolicyOption.GREEDY)
        })
public class Service7ServiceVsSuperInterface {

    @Reference(
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            policyOption = ReferencePolicyOption.GREEDY)
    private volatile List<ServiceInterface3> interfaceDirectList;

    @Reference(
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            policyOption = ReferencePolicyOption.GREEDY)
    private volatile List<ServiceSuperInterface3> superInterfaceDirectList;

    private volatile List<ServiceInterface3> interfaceBindList = new ArrayList<>();
    private volatile List<ServiceSuperInterface3> superInterfaceBindList = new ArrayList<>();

    void bindInterface(ServiceInterface3 service) {
        interfaceBindList.add(service);
    }

    void unbindInterface(ServiceInterface3 service) {
        interfaceBindList.remove(service);
    }

    void bindSuperInterface(ServiceSuperInterface3 service) {
        superInterfaceBindList.add(service);
    }

    void unbindSuperInterface(ServiceSuperInterface3 service) {
        superInterfaceBindList.remove(service);
    }

    public List<ServiceInterface3> getInterfaceDirectList() {
        return interfaceDirectList;
    }

    public List<ServiceSuperInterface3> getSuperInterfaceDirectList() {
        return superInterfaceDirectList;
    }

    public List<ServiceInterface3> getInterfaceBindList() {
        return interfaceBindList;
    }

    public List<ServiceSuperInterface3> getSuperInterfaceBindList() {
        return superInterfaceBindList;
    }
}
