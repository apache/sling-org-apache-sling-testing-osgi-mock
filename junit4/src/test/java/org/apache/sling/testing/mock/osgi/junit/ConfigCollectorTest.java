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
package org.apache.sling.testing.mock.osgi.junit;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Map;

import org.apache.sling.testing.mock.osgi.MapUtil;
import org.apache.sling.testing.mock.osgi.config.annotations.AutoConfig;
import org.apache.sling.testing.mock.osgi.config.annotations.ConfigType;
import org.apache.sling.testing.mock.osgi.config.annotations.SetConfig;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.propertytypes.ServiceRanking;
import org.osgi.service.component.propertytypes.ServiceVendor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@SetConfig(
        pid = "common-config",
        property = {"service.ranking:Integer=42", "service.vendor=Acme Software Foundation"})
@SetConfig(
        component = Object.class,
        property = {"service.ranking:Integer=55", "service.vendor=Eclipse"})
@AutoConfig(List.class)
@ConfigType(type = ServiceRanking.class, property = "service.ranking:Integer=10")
@RunWith(MockitoJUnitRunner.class)
public class ConfigCollectorTest {

    @Rule
    public OsgiContext osgiContext = new OsgiContextBuilder().build();

    @Rule
    public ConfigCollector configCollector = new ConfigCollector(osgiContext);

    @Rule
    public ConfigCollector commonConfigs = new ConfigCollector(osgiContext, "common-config");

    @Rule
    public ConfigCollector objectConfigs = new ConfigCollector(osgiContext, Object.class);

    @ConfigType(type = ServiceVendor.class, lenient = true)
    @Test
    public void testEvaluate() {
        assertEquals(10, configCollector.firstConfig(ServiceRanking.class).value());
        assertNull(configCollector.firstConfig(ServiceVendor.class).value());

        assertEquals(10, configCollector.firstConfigMap(ServiceRanking.class).get("service.ranking"));
        assertNull(configCollector.firstConfigMap(ServiceVendor.class).get("service.vendor"));

        assertEquals(42, commonConfigs.firstConfig(ServiceRanking.class).value());
        assertEquals(
                "Acme Software Foundation",
                commonConfigs.firstConfig(ServiceVendor.class).value());

        assertEquals(42, commonConfigs.firstConfigMap(ServiceRanking.class).get("service.ranking"));
        assertNull(commonConfigs.firstConfigMap(ServiceVendor.class).get("Acme Software Foundation"));

        assertEquals(55, objectConfigs.firstConfig(ServiceRanking.class).value());
        assertEquals("Eclipse", objectConfigs.firstConfig(ServiceVendor.class).value());

        assertEquals(55, objectConfigs.firstConfigMap(ServiceRanking.class).get("service.ranking"));
        assertNull(objectConfigs.firstConfigMap(ServiceVendor.class).get("Eclipse"));
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface ListConfig {
        int size();

        boolean reverse();
    }

    @Test
    @ListConfig(size = 10, reverse = false)
    @SuppressWarnings("null")
    public void autoConfig1() throws Exception {
        ConfigurationAdmin configurationAdmin = osgiContext.getService(ConfigurationAdmin.class);
        assertEquals(
                Map.of("size", 10, "reverse", false, "service.pid", List.class.getName()),
                MapUtil.toMap(configurationAdmin
                        .getConfiguration(List.class.getName())
                        .getProperties()));
    }

    @Test
    @ListConfig(size = 12, reverse = true)
    @SuppressWarnings("null")
    public void autoConfig2() throws Exception {
        ConfigurationAdmin configurationAdmin = osgiContext.getService(ConfigurationAdmin.class);
        assertEquals(
                Map.of("size", 12, "reverse", true, "service.pid", List.class.getName()),
                MapUtil.toMap(configurationAdmin
                        .getConfiguration(List.class.getName())
                        .getProperties()));
    }

    @Test
    @AutoConfig(Void.class) // overrides class annotation with unset config pid
    @ListConfig(size = 12, reverse = true)
    @SuppressWarnings("null")
    public void autoConfigVoid() throws Exception {
        ConfigurationAdmin configurationAdmin = osgiContext.getService(ConfigurationAdmin.class);
        assertNull(MapUtil.toMap(
                configurationAdmin.getConfiguration(List.class.getName()).getProperties()));
    }
}
