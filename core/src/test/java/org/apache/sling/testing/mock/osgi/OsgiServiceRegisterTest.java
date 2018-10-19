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

import static org.junit.Assert.assertEquals;

import org.apache.sling.testing.mock.osgi.OsgiServiceUtilTest.Service2;
import org.apache.sling.testing.mock.osgi.OsgiServiceUtilTest.ServiceInterface2;
import org.apache.sling.testing.mock.osgi.OsgiServiceUtilTest.ServiceInterface3;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.Rule;
import org.junit.Test;

public class OsgiServiceRegisterTest {

    @Rule
    public OsgiContext context = new OsgiContext();

    @Test
    public void testRegisterClassesFromMetadata() {
        context.registerInjectActivateService(new Service2());
        
        assertEquals(1, context.getServices(ServiceInterface2.class, null).length);
        assertEquals(1, context.getServices(ServiceInterface3.class, null).length);
    }

    @Test
    public void testRegisterExplicitClass1() {
        Service2 service = new Service2();
        MockOsgi.injectServices(service, context.bundleContext());
        MockOsgi.activate(service, context.bundleContext());
        context.registerService(ServiceInterface2.class, service);

        assertEquals(1, context.getServices(ServiceInterface2.class, null).length);
        assertEquals(0, context.getServices(ServiceInterface3.class, null).length);
    }

    @Test
    public void testRegisterExplicitClass2() {
        Service2 service = new Service2();
        MockOsgi.injectServices(service, context.bundleContext());
        MockOsgi.activate(service, context.bundleContext());
        context.registerService(ServiceInterface3.class, service);

        assertEquals(0, context.getServices(ServiceInterface2.class, null).length);
        assertEquals(1, context.getServices(ServiceInterface3.class, null).length);
    }

    @Test
    public void testRegisterExplicitClass3() {
        Service2 service = new Service2();
        MockOsgi.injectServices(service, context.bundleContext());
        MockOsgi.activate(service, context.bundleContext());
        context.registerService(ServiceInterface2.class, service);
        context.registerService(ServiceInterface3.class, service);

        assertEquals(1, context.getServices(ServiceInterface2.class, null).length);
        assertEquals(1, context.getServices(ServiceInterface3.class, null).length);
    }
        
}
