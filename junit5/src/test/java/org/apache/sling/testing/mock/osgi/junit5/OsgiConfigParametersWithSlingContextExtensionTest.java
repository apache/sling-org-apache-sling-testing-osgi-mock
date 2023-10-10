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
package org.apache.sling.testing.mock.osgi.junit5;

import org.apache.sling.testing.mock.osgi.MapUtil;
import org.apache.sling.testing.mock.osgi.config.annotations.ApplyConfig;
import org.apache.sling.testing.mock.osgi.config.annotations.UpdateConfig;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.Service3OsgiR6Impl;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.ServiceInterface1;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.ServiceInterface2;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit5.SlingContext;
import org.apache.sling.testing.mock.sling.junit5.SlingContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.propertytypes.ServiceRanking;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests to ensure that {@link OsgiConfigParametersExtension} works with other extensions providing their own
 * {@link org.apache.sling.testing.mock.osgi.context.OsgiContextImpl} subtypes.
 */
@ExtendWith({OsgiConfigParametersExtension.class, SlingContextExtension.class, MockitoExtension.class})
class OsgiConfigParametersWithSlingContextExtensionTest {
    private static final String PID_SETUP = "setup-pid";

    private final SlingContext context = new SlingContext(ResourceResolverType.JCR_MOCK);

    @Mock
    private ServiceInterface1 dependency1a;

    @Mock
    private ServiceInterface2 dependency2a;

    @BeforeEach
    void setUp() throws Exception {
        ConfigurationAdmin configurationAdmin = context.getService(ConfigurationAdmin.class);
        configurationAdmin.getConfiguration(PID_SETUP).update(MapUtil.toDictionary(Map.of("service.ranking", "55")));
    }

    @Test
    @UpdateConfig(component = Service3OsgiR6Impl.class, property = {
            "reference3DynamicFiltered.target=(prop1=def)",
            "service.ranking:Integer=42"
    })
    void testUpdateConfig() {
        BundleContext bundleContext = context.bundleContext();

        // setup service instance with only minimum mandatory references
        ServiceRegistration reg1a = bundleContext.registerService(ServiceInterface1.class.getName(), dependency1a, null);
        ServiceRegistration reg2a = bundleContext.registerService(ServiceInterface2.class.getName(), dependency2a, null);

        Service3OsgiR6Impl service = new Service3OsgiR6Impl();
        context.registerInjectActivateService(service);

        assertSame(dependency1a, service.getReference1());
        assertNull(service.getReference1Optional());
        assertEquals(Set.of(dependency2a), Set.copyOf(service.getReferences2()));
        assertEquals(Collections.emptySet(), Set.copyOf(service.getReferences3()));

        assertEquals(42, service.getConfig().get("service.ranking"));
    }

    @Test
    @ApplyConfig(pid = PID_SETUP, type = ServiceRanking.class)
    void testApplyConfig(ServiceRanking serviceRanking) {
        assertEquals(55, serviceRanking.value());
    }
}
