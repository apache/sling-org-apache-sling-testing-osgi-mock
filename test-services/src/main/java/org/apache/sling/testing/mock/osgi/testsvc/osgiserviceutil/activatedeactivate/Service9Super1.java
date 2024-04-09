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

import java.util.Map;

import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.ServiceInterface1;

/**
 * SLING-11860 - Superclass that provides generic activate/deactivate/bind/unbind methods
 */
public abstract class Service9Super1 implements ServiceInterface1 {

    protected Class<?> activateFromClass = null;
    protected Class<?> deactivateFromClass = null;
    protected Class<?> bindSvc1FromClass = null;
    protected Class<?> unbindSvc1FromClass = null;

    protected void activate(Map<String, Object> props) {
        activateFromClass = Service9Super1.class;
    }

    protected void deactivate(Map<String, Object> props) {
        deactivateFromClass = Service9Super1.class;
    }

    protected void bindServiceInterface1(ServiceInterface1 svc1) {
        bindSvc1FromClass = Service9Super1.class;
    }

    protected void unbindServiceInterface1(ServiceInterface1 svc1) {
        unbindSvc1FromClass = Service9Super1.class;
    }

    public Class<?> getActivateFromClass() {
        return activateFromClass;
    }

    public Class<?> getDeactivateFromClass() {
        return deactivateFromClass;
    }

    public Class<?> getBindSvc1FromClass() {
        return bindSvc1FromClass;
    }

    public Class<?> getUnbindSvc1FromClass() {
        return unbindSvc1FromClass;
    }
}
