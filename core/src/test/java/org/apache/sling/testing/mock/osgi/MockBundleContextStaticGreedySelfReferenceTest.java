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
package org.apache.sling.testing.mock.osgi;

import java.util.Collection;

import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.ServiceInterface3;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.ServiceInterface3Impl;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.ServiceInterface3ImplSelfReferencing;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import static org.junit.Assert.assertNotNull;

/**
 * Test cases for SLING-10616
 */
@RunWith(MockitoJUnitRunner.class)
public class MockBundleContextStaticGreedySelfReferenceTest {

    private BundleContext bundleContext;

    @Before
    public void setUp() {
        bundleContext = MockOsgi.newBundleContext();
    }

    @Test
    public void testSelfReferenceWithTargetFilter() throws InvalidSyntaxException {
        MockOsgi.registerInjectActivateService(ServiceInterface3Impl.class, bundleContext);
        MockOsgi.registerInjectActivateService(ServiceInterface3ImplSelfReferencing.class, bundleContext);

        assertNotNull(getDefaultImplFromReference());
    }

    @Test
    public void testSelfReferenceWithTargetFilterReverse() throws InvalidSyntaxException {
        MockOsgi.registerInjectActivateService(ServiceInterface3ImplSelfReferencing.class, bundleContext);
        MockOsgi.registerInjectActivateService(ServiceInterface3Impl.class, bundleContext);

        assertNotNull(getDefaultImplFromReference());
    }

    /**
     * Get injected filtered service from ServiceInterface3ImplSelfReferencing.
     */
    @SuppressWarnings("null")
    private ServiceInterface3 getDefaultImplFromReference() throws InvalidSyntaxException {
        Collection<ServiceReference<ServiceInterface3>> references =
                bundleContext.getServiceReferences(ServiceInterface3.class, "(!(prop1=abc))");
        ServiceInterface3 service =
                bundleContext.getService(references.iterator().next());
        return ((ServiceInterface3ImplSelfReferencing) service).getDefaultImplementation();
    }
}
