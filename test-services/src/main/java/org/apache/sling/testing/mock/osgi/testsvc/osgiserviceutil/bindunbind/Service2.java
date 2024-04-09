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
package org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.bindunbind;

import java.util.ArrayList;
import java.util.List;

import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.ServiceInterface1;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

@Component
public class Service2 {

    @Reference(
            name = "reference1",
            bind = "bindReference1",
            unbind = "unbindReference1",
            service = ServiceInterface1.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            policyOption = ReferencePolicyOption.GREEDY)
    private volatile List<ServiceReference<ServiceInterface1>> references = new ArrayList<>();

    void bindReference1(ServiceReference<ServiceInterface1> reference) {
        references.add(reference);
    }

    void unbindReference1(ServiceReference<ServiceInterface1> reference) {
        references.remove(reference);
    }

    public List<ServiceReference<ServiceInterface1>> getReferences() {
        return references;
    }
}
