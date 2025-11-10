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

import java.io.IOException;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.apache.sling.testing.mock.osgi.OsgiMetadataUtilTest.ServiceWithMetadata;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MockConfigurationAdminTest {

    private static final String[] TEST_ADAPTABLES = new String[] {"adaptable1", "adaptable2"};

    @Rule
    public OsgiContext context = new OsgiContext();

    private ConfigurationAdmin underTest;

    @Before
    public void setUp() {
        underTest = context.getService(ConfigurationAdmin.class);
    }

    @Test
    public void testGetConfigurationString() throws IOException {
        MockOsgi.setConfigForPid(
                context.bundleContext(),
                "org.apache.sling.testing.mock.osgi.OsgiMetadataUtilTest$ServiceWithMetadata",
                Constants.SERVICE_RANKING,
                3000,
                "adaptables",
                TEST_ADAPTABLES,
                "prop2",
                2);

        context.registerInjectActivateService(
                new ServiceWithMetadata(), Map.<String, Object>of(Constants.SERVICE_RANKING, 4000, "prop1", 1));

        ServiceReference reference = context.bundleContext().getServiceReference(Comparable.class.getName());

        // values passed over when registering service has highest precedence
        assertEquals(4000, reference.getProperty(Constants.SERVICE_RANKING));
        assertEquals(1, reference.getProperty("prop1"));

        // values set in config admin has 2ndmost highest precedence
        assertArrayEquals(TEST_ADAPTABLES, (String[]) reference.getProperty("adaptables"));
        assertEquals(2, reference.getProperty("prop2"));

        // values set in OSGi SCR metadata
        assertEquals("The Apache Software Foundation", reference.getProperty(Constants.SERVICE_VENDOR));
        assertEquals(
                "org.apache.sling.testing.mock.osgi.OsgiMetadataUtilTest$ServiceWithMetadata",
                reference.getProperty(Constants.SERVICE_PID));
    }

    @Test
    public void testConfigurationPID() throws IOException {
        MockOsgi.setConfigForPid(
                context.bundleContext(), ServiceWithConfigurationPID.class.getSimpleName(), "prop1", 1);

        context.registerInjectActivateService(new ServiceWithConfigurationPID(), Map.<String, Object>of("prop2", 2));

        ServiceReference reference = context.bundleContext().getServiceReference(Comparable.class.getName());

        assertEquals(1, reference.getProperty("prop1"));
        assertEquals(2, reference.getProperty("prop2"));
    }

    @Test
    public void testMultipleConfigurationPID() throws IOException {
        MockOsgi.setConfigForPid(context.bundleContext(), "Configuration1", "prop1", 1);
        MockOsgi.setConfigForPid(context.bundleContext(), "Configuration2", "prop1", 2);

        context.registerInjectActivateService(
                new ServiceWithMultipleConfigurationPID(), Map.<String, Object>of("prop2", 2));

        ServiceReference reference = context.bundleContext().getServiceReference(Comparable.class.getName());

        assertEquals(2, reference.getProperty("prop1"));
        assertEquals(2, reference.getProperty("prop2"));
    }

    @Test
    public void testFilteringConfigurations() throws IOException, InvalidSyntaxException {
        MockOsgi.setConfigForPid(context.bundleContext(), "Configuration1", "prop1", 1, "prop2", "B");
        MockOsgi.setConfigForPid(context.bundleContext(), "Configuration2", "prop1", 2, "prop2", "A");
        MockOsgi.setConfigForPid(context.bundleContext(), "Configuration3", "prop1", 3, "prop2", "A");

        final Configuration[] allConfigurations = underTest.listConfigurations("(prop1=*)");
        assertEquals(3, allConfigurations.length);
        final Configuration[] prop2AConfigurations = underTest.listConfigurations("(prop2=A)");
        assertEquals(2, prop2AConfigurations.length);
        final Configuration[] searchForAllConfigurations = underTest.listConfigurations(null);
        assertTrue(searchForAllConfigurations.length
                >= 3); // Other configurations could be registered outside this method as well
        final Configuration[] noConfigurations = underTest.listConfigurations("(nonexistingprop=nonexistingvalue)");
        assertNull(noConfigurations);
    }

    @Test
    public void testGetConfigurationViaConfigAdmin_NonExisting() throws IOException {
        Configuration config = underTest.getConfiguration("Configuration1");
        assertNotNull(config);
        assertEquals("Configuration1", config.getPid());

        config = underTest.getConfiguration("Configuration1", "location1");
        assertNotNull(config);
        assertEquals("Configuration1", config.getPid());
    }

    @Test
    public void testGetConfigurationViaConfigAdmin_Existing() throws IOException {
        MockOsgi.setConfigForPid(context.bundleContext(), "Configuration1", "prop1", 1);

        Configuration config = underTest.getConfiguration("Configuration1");
        assertNotNull(config);
        assertEquals("Configuration1", config.getPid());
        assertEquals(1, config.getProperties().get("prop1"));

        config = underTest.getConfiguration("Configuration1", "location1");
        assertNotNull(config);
        assertEquals("Configuration1", config.getPid());
        assertEquals(1, config.getProperties().get("prop1"));
    }

    @Test
    public void testGetUpdateDeleteGetConfiguration() throws IOException {
        Configuration configurationNew = underTest.getConfiguration("new-pid");
        assertNull(configurationNew.getProperties());
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put("key", "value");
        configurationNew.update(properties);

        Configuration configurationExisting = underTest.getConfiguration("new-pid");
        assertEquals("value", configurationNew.getProperties().get("key"));
        assertNotNull(configurationExisting.getProperties().get(Constants.SERVICE_PID));
        assertNull(configurationExisting.getProperties().get(ConfigurationAdmin.SERVICE_FACTORYPID));
        configurationExisting.delete();

        Configuration configurationDeleted = underTest.getConfiguration("new-pid");
        assertNull(configurationDeleted.getProperties());
    }

    @Test
    public void testUpdateIfDifferent() throws IOException {
        Configuration configurationNew = underTest.getConfiguration("new-pid");
        assertNull(configurationNew.getProperties());
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put("key", "value");
        properties.put("keyArray", new String[] {"A", "B"});
        properties.put("keyCollection", Arrays.asList("X", "Y"));
        configurationNew.update(properties);

        Dictionary<String, Object> propertiesNew = new Hashtable<String, Object>(MapUtil.toMap(properties));
        assertFalse(configurationNew.updateIfDifferent(propertiesNew));
        propertiesNew.put("keyArray", new String[] {"A", "B"});
        propertiesNew.put("keyCollection", Arrays.asList("X", "Y"));
        assertFalse(configurationNew.updateIfDifferent(propertiesNew));
        propertiesNew.put("keyArray", new String[] {"A", "B", "C"});
        assertTrue(configurationNew.updateIfDifferent(propertiesNew));
        propertiesNew.put("keyCollection", Arrays.asList("X", "Y", "Z"));
        assertTrue(configurationNew.updateIfDifferent(propertiesNew));
    }

    @Test
    public void testNoArgUpdateAfterGetFactoryConfiguration() throws IOException {
        Configuration configurationNew = underTest.getFactoryConfiguration("my.factory1", "name1");
        assertNull(configurationNew.getProperties());
        configurationNew.update();

        Configuration configurationExisting = underTest.getFactoryConfiguration("my.factory1", "name1");
        assertNotNull(configurationExisting.getProperties().get(Constants.SERVICE_PID));
        assertNotNull(configurationExisting.getProperties().get(ConfigurationAdmin.SERVICE_FACTORYPID));
    }

    @Test
    public void testGetUpdateDeleteGetFactoryConfiguration() throws IOException {
        Configuration configurationNew = underTest.getFactoryConfiguration("my.factory1", "name1");
        assertNull(configurationNew.getProperties());
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("key", "value");
        configurationNew.update(properties);

        Configuration configurationExisting = underTest.getFactoryConfiguration("my.factory1", "name1");
        assertEquals("value", configurationNew.getProperties().get("key"));
        assertNotNull(configurationExisting.getProperties().get(Constants.SERVICE_PID));
        assertNotNull(configurationExisting.getProperties().get(ConfigurationAdmin.SERVICE_FACTORYPID));
        configurationExisting.delete();

        Configuration configurationDeleted = underTest.getFactoryConfiguration("my.factory1", "name1");
        assertNull(configurationDeleted.getProperties());
    }

    @Test
    public void testGetFactoryConfigurationViaConfigAdmin_NonExisting() throws IOException {
        Configuration config = underTest.getFactoryConfiguration("my.factory1", "name1");
        assertNotNull(config);
        assertEquals("my.factory1~name1", config.getPid());
        assertEquals("my.factory1", config.getFactoryPid());

        config = underTest.getFactoryConfiguration("my.factory2", "name2", "location1");
        assertNotNull(config);
        assertEquals("my.factory2~name2", config.getPid());
        assertEquals("my.factory2", config.getFactoryPid());
    }

    @Test
    public void testGetFactoryConfigurationViaConfigAdmin_Existing() throws IOException {
        MockOsgi.setFactoryConfigForPid(context.bundleContext(), "my.factory3", "name3", "prop1", 1);

        Configuration config = underTest.getFactoryConfiguration("my.factory3", "name3");
        assertNotNull(config);
        assertEquals("my.factory3~name3", config.getPid());
        assertEquals("my.factory3", config.getFactoryPid());
        assertEquals(1, config.getProperties().get("prop1"));

        MockOsgi.setFactoryConfigForPid(context.bundleContext(), "my.factory4", "name4", Map.of("prop1", 2));
        config = underTest.getFactoryConfiguration("my.factory4", "name4", "location1");
        assertNotNull(config);
        assertEquals("my.factory4~name4", config.getPid());
        assertEquals("my.factory4", config.getFactoryPid());
        assertEquals(2, config.getProperties().get("prop1"));
    }

    static class ServiceWithConfigurationPID {}

    static class ServiceWithMultipleConfigurationPID {}
}
