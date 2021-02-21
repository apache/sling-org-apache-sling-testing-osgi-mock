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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.sling.testing.mock.osgi.OsgiMetadataUtilTest.ServiceWithMetadata;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import com.google.common.collect.ImmutableMap;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class MockConfigurationAdminTest {
    
    private static final String[] TEST_ADAPTABLES = new String[] {
        "adaptable1",
        "adaptable2"
    };

    @Rule
    public OsgiContext context = new OsgiContext();

    @Test
    public void testGetConfigurationString() throws IOException {
        MockOsgi.setConfigForPid(context.bundleContext(), "org.apache.sling.testing.mock.osgi.OsgiMetadataUtilTest$ServiceWithMetadata",
                Constants.SERVICE_RANKING, 3000,
                "adaptables", TEST_ADAPTABLES,
                "prop2", 2);
        
        context.registerInjectActivateService(new ServiceWithMetadata(), ImmutableMap.<String, Object>builder()
                .put(Constants.SERVICE_RANKING, 4000)
                .put("prop1", 1)
                .build());
        
        ServiceReference reference = context.bundleContext().getServiceReference(Comparable.class.getName());

        // values passed over when registering service has highest precedence
        assertEquals(4000, reference.getProperty(Constants.SERVICE_RANKING));
        assertEquals(1, reference.getProperty("prop1"));

        // values set in config admin has 2ndmost highest precedence
        assertArrayEquals(TEST_ADAPTABLES, (String[])reference.getProperty("adaptables"));
        assertEquals(2, reference.getProperty("prop2"));

        // values set in OSGi SCR metadata
        assertEquals("The Apache Software Foundation", reference.getProperty(Constants.SERVICE_VENDOR));
        assertEquals("org.apache.sling.testing.mock.osgi.OsgiMetadataUtilTest$ServiceWithMetadata", reference.getProperty(Constants.SERVICE_PID));
    }

    @Test
    public void testConfigurationPID() throws IOException {
        MockOsgi.setConfigForPid(context.bundleContext(), ServiceWithConfigurationPID.class.getSimpleName(),
                "prop1", 1);

        context.registerInjectActivateService(new ServiceWithConfigurationPID(), ImmutableMap.<String, Object>builder()
                .put("prop2", 2)
                .build());

        ServiceReference reference = context.bundleContext().getServiceReference(Comparable.class.getName());

        assertEquals(1, reference.getProperty("prop1"));
        assertEquals(2, reference.getProperty("prop2"));
    }

    @Test
    public void testMultipleConfigurationPID() throws IOException {
        MockOsgi.setConfigForPid(context.bundleContext(), "Configuration1",
                "prop1", 1);
        MockOsgi.setConfigForPid(context.bundleContext(), "Configuration2",
                "prop1", 2);

        context.registerInjectActivateService(new ServiceWithMultipleConfigurationPID(), ImmutableMap.<String, Object>builder()
                .put("prop2", 2)
                .build());

        ServiceReference reference = context.bundleContext().getServiceReference(Comparable.class.getName());

        assertEquals(2, reference.getProperty("prop1"));
        assertEquals(2, reference.getProperty("prop2"));
    }

    @Test
    public void testFilteringConfigurations() throws IOException, InvalidSyntaxException {
        MockOsgi.setConfigForPid(context.bundleContext(), "Configuration1", "prop1", 1, "prop2", "B");
        MockOsgi.setConfigForPid(context.bundleContext(), "Configuration2", "prop1", 2, "prop2", "A");
        MockOsgi.setConfigForPid(context.bundleContext(), "Configuration3", "prop1", 3, "prop2", "A");

        final ConfigurationAdmin configurationAdmin = context.bundleContext().getService(context.bundleContext().getServiceReference(ConfigurationAdmin.class));
        final Configuration[] allConfigurations = configurationAdmin.listConfigurations("(prop1=*)");
        assertEquals(3, allConfigurations.length);
        final Configuration[] prop2AConfigurations = configurationAdmin.listConfigurations("(prop2=A)");
        assertEquals(2, prop2AConfigurations.length);
        final Configuration[] searchForAllConfigurations = configurationAdmin.listConfigurations(null);
        assertTrue(searchForAllConfigurations.length >= 3); // Other configurations could be registered outside this method as well
        final Configuration[] noConfigurations = configurationAdmin.listConfigurations("(nonexistingprop=nonexistingvalue)");
        assertNull(noConfigurations);
    }

    static class ServiceWithConfigurationPID {}

    static class ServiceWithMultipleConfigurationPID {}
}
