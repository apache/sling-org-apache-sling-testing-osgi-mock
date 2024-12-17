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

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.RankedService;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.RankedServiceFive;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.RankedServiceTen;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.Service6VolatileMultipleReferences;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

/**
 * Test the service-ranking based sorting of mock service references
 */
@SuppressWarnings("null")
public class MockServiceReferencesSortTest {

    private BundleContext bundleContext;

    @Before
    public void setUp() {
        bundleContext = MockOsgi.newBundleContext();
    }

    @After
    public void tearDown() {
        MockOsgi.shutdown(bundleContext);
    }

    @Test
    public void testAllWithRanking() {
        registerStringServiceWithRanking("A", 3);
        registerStringServiceWithRanking("B", 5);
        registerStringServiceWithRanking("C", 4);
        registerStringServiceWithRanking("D", 1);
        registerStringServiceWithRanking("E", 2);

        assertEquals("DEACB", getSortedServicesString());
        assertEquals("B", bundleContext.getService(bundleContext.getServiceReference(String.class)));
    }

    @Test
    public void testAllWithoutRanking() {
        registerStringServiceWithoutRanking("A");
        registerStringServiceWithoutRanking("B");
        registerStringServiceWithoutRanking("C");
        registerStringServiceWithoutRanking("D");
        registerStringServiceWithoutRanking("E");

        assertEquals("EDCBA", getSortedServicesString());
        assertEquals("A", bundleContext.getService(bundleContext.getServiceReference(String.class)));
    }

    @Test
    public void testMixed() {
        registerStringServiceWithoutRanking("A");
        registerStringServiceWithRanking("B", 5);
        registerStringServiceWithoutRanking("C");
        registerStringServiceWithRanking("D", 10);
        registerStringServiceWithoutRanking("E");

        assertEquals("ECABD", getSortedServicesString());
        assertEquals("D", bundleContext.getService(bundleContext.getServiceReference(String.class)));
    }

    @Test
    public void testVolatileCollectionReference() {
        RankedService rankedServiceTen = new RankedServiceTen();
        bundleContext.registerService(RankedService.class.getName(), rankedServiceTen, null);
        MockOsgi.activate(rankedServiceTen, bundleContext);

        RankedService rankedServiceFive = new RankedServiceFive();
        bundleContext.registerService(RankedService.class.getName(), rankedServiceFive, null);
        MockOsgi.activate(rankedServiceFive, bundleContext);

        Service6VolatileMultipleReferences service6VolatileMultipleReferences =
                new Service6VolatileMultipleReferences();
        bundleContext.registerService(
                Service6VolatileMultipleReferences.class.getName(), service6VolatileMultipleReferences, null);
        MockOsgi.injectServices(service6VolatileMultipleReferences, bundleContext);

        assertEquals(
                "Should get highest when getting one service",
                rankedServiceTen,
                bundleContext.getService(bundleContext.getServiceReference(RankedService.class)));
        assertEquals(
                "Should have order from lowest to highest on sorted ranked services",
                "RankedServiceFive=5RankedServiceTen=10",
                getSortedRankedServices());
        assertEquals(
                "Should have order from lowest to highest on volatile reference list",
                "RankedServiceFive=5RankedServiceTen=10",
                service6VolatileMultipleReferences.getRanks());
    }

    @Test
    public void testConsistentRankedServicesSorting() {
        // random order of instantiation and registration
        RankedService serviceRanking0 = mock(RankedService.class);
        bundleContext.registerService(RankedService.class, serviceRanking0, createServiceRankingDictionary(0));
        RankedService serviceRankingMin5 = mock(RankedService.class);
        bundleContext.registerService(RankedService.class, serviceRankingMin5, createServiceRankingDictionary(-5));
        RankedService serviceRanking5 = mock(RankedService.class);
        bundleContext.registerService(RankedService.class, serviceRanking5, createServiceRankingDictionary(5));

        Service6VolatileMultipleReferences service =
                MockOsgi.registerInjectActivateService(Service6VolatileMultipleReferences.class, bundleContext);
        List<RankedService> referencedServices = service.getRankedServices();
        assertNotNull(referencedServices);
        assertEquals(3, referencedServices.size());
        assertEquals(serviceRankingMin5, referencedServices.get(0));
        assertEquals(serviceRanking0, referencedServices.get(1));
        assertEquals(serviceRanking5, referencedServices.get(2));

        // register some more services
        RankedService serviceRankingMin999 = mock(RankedService.class);
        bundleContext.registerService(RankedService.class, serviceRankingMin999, createServiceRankingDictionary(-999));
        RankedService serviceRanking1 = mock(RankedService.class);
        bundleContext.registerService(RankedService.class, serviceRanking1, createServiceRankingDictionary(1));

        // check that the collection has been updated and sorted correctly
        assertEquals(5, referencedServices.size());
        assertEquals(serviceRankingMin999, referencedServices.get(0));
        assertEquals(serviceRankingMin5, referencedServices.get(1));
        assertEquals(serviceRanking0, referencedServices.get(2));
        assertEquals(serviceRanking1, referencedServices.get(3));
        assertEquals(serviceRanking5, referencedServices.get(4));
    }

    private Dictionary<String, Object> createServiceRankingDictionary(int serviceRanking) {
        return new Hashtable<>(Map.of(Constants.SERVICE_RANKING, serviceRanking));
    }

    private String getSortedRankedServices() {
        ServiceReference<?>[] refs = null;
        try {
            refs = bundleContext.getServiceReferences(RankedService.class.getName(), null);
        } catch (InvalidSyntaxException ise) {
            fail("Unexpected InvalidSyntaxException");
        }
        assertNotNull("Expecting our service references", refs);
        Arrays.sort(refs);

        final StringBuilder sb = new StringBuilder();
        for (ServiceReference<?> ref : refs) {
            RankedService rankedService = (RankedService) bundleContext.getService(ref);
            sb.append(rankedService.getClass().getSimpleName()).append("=").append(rankedService.getRanking());
            bundleContext.ungetService(ref);
        }

        return sb.toString();
    }

    private ServiceRegistration<?> registerStringServiceWithoutRanking(String serviceValue) {
        return bundleContext.registerService(String.class, serviceValue, new Hashtable<String, Object>());
    }

    private ServiceRegistration<?> registerStringServiceWithRanking(String serviceValue, int index) {
        final Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_RANKING, Integer.valueOf(index));
        return bundleContext.registerService(String.class, serviceValue, props);
    }

    /** Register services with a specific ranking, sort their references and
     *  return their concatenated toString() values.
     *  Use to test service references sorting.
     */
    private String getSortedServicesString() {
        ServiceReference<?>[] refs = null;
        try {
            refs = bundleContext.getServiceReferences(String.class.getName(), null);
        } catch (InvalidSyntaxException ise) {
            fail("Unexpected InvalidSyntaxException");
        }
        assertNotNull("Expecting our service references", refs);
        Arrays.sort(refs);

        final StringBuilder sb = new StringBuilder();
        for (ServiceReference<?> ref : refs) {
            sb.append(bundleContext.getService(ref).toString());
            bundleContext.ungetService(ref);
        }

        return sb.toString();
    }
}
