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

import java.util.concurrent.atomic.AtomicLong;

import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.ScopePrototypeServiceFactory.ScopePrototpyeInstance;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

@Component(service = ScopePrototypeServiceFactory.class, scope = ServiceScope.PROTOTYPE)
public class ScopePrototypeServiceFactory implements ServiceFactory<ScopePrototpyeInstance> {

    private static final AtomicLong INSTANCE_COUNTER = new AtomicLong();

    @Override
    public ScopePrototpyeInstance getService(Bundle bundle, ServiceRegistration<ScopePrototpyeInstance> registration) {
        return new ScopePrototpyeInstance(INSTANCE_COUNTER.incrementAndGet());
    }

    @Override
    public void ungetService(Bundle bundle, ServiceRegistration<ScopePrototpyeInstance> registration,
            ScopePrototpyeInstance service) {
        // nothing to do
    }

    public static class ScopePrototpyeInstance {

        private final long instanceId;

        private ScopePrototpyeInstance(long instanceId) {
            this.instanceId = instanceId;
        }

        public long getInstanceId() {
            return instanceId;
        }

    }

}
