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

import org.apache.sling.testing.mock.osgi.config.annotations.ConfigType;
import org.apache.sling.testing.mock.osgi.config.annotations.UpdateConfig;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.osgi.service.component.propertytypes.ServiceRanking;
import org.osgi.service.component.propertytypes.ServiceVendor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@UpdateConfig(pid = "common-config", property = {
        "service.ranking:Integer=42",
        "service.vendor=Acme Software Foundation"
})
@ConfigType(type = ServiceRanking.class, property = "service.ranking:Integer=10")
@RunWith(MockitoJUnitRunner.class)
public class ConfigCollectorTest {

    @Rule
    public OsgiContext osgiContext = new OsgiContextBuilder().build();

    @Rule
    public ConfigCollector configCollector = new ConfigCollector(osgiContext,
            ServiceRanking.class, ServiceVendor.class);

    @Rule
    public ConfigCollector justRankings = new ConfigCollector(osgiContext, ServiceRanking.class);

    @Rule
    public ConfigCollector appliedConfigs = new ConfigCollector(osgiContext, "common-config",
            ServiceRanking.class, ServiceVendor.class);

    @ConfigType(type = ServiceVendor.class)
    @Test
    public void testEvaluate() {
        assertEquals(1, justRankings.stream().count());
        assertEquals(2, configCollector.stream().count());
        assertEquals(10, configCollector.configStream(ServiceRanking.class).findFirst().orElseThrow().value());
        assertNull(configCollector.configStream(ServiceVendor.class).findFirst().orElseThrow().value());

        assertEquals(2, appliedConfigs.stream().count());
        assertEquals(42, appliedConfigs.configStream(ServiceRanking.class).findFirst().orElseThrow().value());
        assertEquals("Acme Software Foundation",
                appliedConfigs.configStream(ServiceVendor.class).findFirst().orElseThrow().value());
    }
}
