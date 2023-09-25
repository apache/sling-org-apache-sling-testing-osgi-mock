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

import org.apache.sling.testing.mock.osgi.MapUtil;
import org.apache.sling.testing.mock.osgi.config.annotations.ApplyConfig;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.propertytypes.ServiceRanking;
import org.osgi.service.component.propertytypes.ServiceVendor;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@ApplyConfig(value = ServiceRanking.class, property = "service.ranking:Integer=10")
@RunWith(MockitoJUnitRunner.class)
public class ConfigCollectorTest {

    @Rule
    public OsgiContext osgiContext = new OsgiContextBuilder().afterSetUp(context -> {
        ConfigurationAdmin configAdmin = context.getService(ConfigurationAdmin.class);
        assertNotNull(configAdmin);
        Configuration configuration = configAdmin.getConfiguration("common-config");
        configuration.update(MapUtil.toDictionary(Map.of(
                "service.ranking", 42,
                "service.vendor", "Apache")));
    }).build();

    @Rule
    public ConfigCollector configCollector = new ConfigCollector(osgiContext,
            ServiceRanking.class, ServiceVendor.class);

    @Rule
    public ConfigCollector justRankings = new ConfigCollector(osgiContext, ServiceRanking.class);

    @Rule
    public ConfigCollector appliedConfigs = new ConfigCollector(osgiContext, "common-config",
            ServiceRanking.class, ServiceVendor.class);

    @ApplyConfig(ServiceVendor.class)
    @Test
    public void testEvaluate() {
        assertEquals(1, justRankings.stream().count());
        assertEquals(2, configCollector.stream().count());
        assertEquals(10, configCollector.configStream(ServiceRanking.class).findFirst().orElseThrow().value());
        assertNull(configCollector.configStream(ServiceVendor.class).findFirst().orElseThrow().value());

        assertEquals(2, appliedConfigs.stream().count());
        assertEquals(42, appliedConfigs.configStream(ServiceRanking.class).findFirst().orElseThrow().value());
        assertEquals("Apache", appliedConfigs.configStream(ServiceVendor.class).findFirst().orElseThrow().value());
    }
}
