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

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

@Component
public class Service6Constructor {

    private boolean activated;
    private ComponentContext componentContext;
    private BundleContext bundleContext;
    private Map<String, Object> map;

    @Activate
    public Service6Constructor(
            ComponentContext componentContext, BundleContext bundleContext, Map<String, Object> map) {
        this.activated = true;
        this.componentContext = componentContext;
        this.bundleContext = bundleContext;
        this.map = map;
    }

    @Deactivate
    private void deactivate(Map<String, Object> map, BundleContext bundleContext, int value1, Integer value2) {
        this.activated = false;
        this.componentContext = null;
        this.bundleContext = null;
        this.map = null;
    }

    public boolean isActivated() {
        return activated;
    }

    public ComponentContext getComponentContext() {
        return componentContext;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public Map<String, Object> getMap() {
        return map;
    }
}
