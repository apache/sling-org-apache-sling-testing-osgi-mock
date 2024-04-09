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
package org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.activatedeactivate;

import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.ServiceInterface1;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * SLING-11860 - subclass to provide more specific activate/deactivate/bind/unbind methods
 */
@Component(service = ServiceInterface1.class)
public class Service9 extends Service9Super1 {

    @Activate
    protected void activate(ServiceConfig config) {
        activateFromClass = Service9.class;
    }

    @Deactivate
    protected void deactivate(ServiceConfig config) {
        deactivateFromClass = Service9.class;
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL)
    protected void bindServiceInterface1(ServiceInterface1 svc1, ServiceReference<ServiceInterface1> svc1Ref) {
        bindSvc1FromClass = Service9.class;
    }

    protected void unbindServiceInterface1(ServiceInterface1 svc1, ServiceReference<ServiceInterface1> svc1Ref) {
        unbindSvc1FromClass = Service9.class;
    }
}
