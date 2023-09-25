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
import org.apache.sling.testing.mock.osgi.config.annotations.ConfigCollection;
import org.apache.sling.testing.mock.osgi.config.annotations.ApplyConfig;
import org.apache.sling.testing.mock.osgi.config.annotations.TypedConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.propertytypes.ServiceRanking;
import org.osgi.service.component.propertytypes.ServiceVendor;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ApplyConfig(value = ServiceVendor.class, property = "service.vendor=Apache Software Foundation")
@ExtendWith({OsgiConfigParametersExtension.class, OsgiContextExtension.class})
class ConfigCollectionImplTest {

    public OsgiContext context = new OsgiContextBuilder().afterSetUp(context -> {
        ConfigurationAdmin configAdmin = context.getService(ConfigurationAdmin.class);
        assertNotNull(configAdmin);
        Configuration configuration = configAdmin.getConfiguration("common-config");
        configuration.update(MapUtil.toDictionary(Map.of(
                "service.ranking", 42,
                "service.vendor", "Acme Software Foundation")));
    }).build();

    @SuppressWarnings("unchecked")
    @ApplyConfig(ServiceRanking.class)
    @Test
    void collectConfigTypes(@CollectConfigTypes(ServiceRanking.class)
                            ConfigCollection configs) {
        assertTrue(configs.stream().map(TypedConfig::getType).anyMatch(ServiceRanking.class::isAssignableFrom));
        assertTrue(configs.stream().map(TypedConfig::getType).noneMatch(ApplyConfig.class::isAssignableFrom));
    }

    @ApplyConfig(value = ServiceRanking.class, property = "service.ranking:Integer=10")
    @Test
    void collectWithApplyPid(
            @CollectConfigTypes(value = {ServiceRanking.class, ServiceVendor.class})
            ConfigCollection unappliedConfigs,
            @CollectConfigTypes(value = {ServiceRanking.class, ServiceVendor.class}, applyPid = "common-config")
            ConfigCollection appliedConfigs) {
        assertEquals(2, unappliedConfigs.stream().count());
        assertEquals(10,
                unappliedConfigs.configStream(ServiceRanking.class).findFirst().orElseThrow().value());
        assertEquals("Apache Software Foundation",
                unappliedConfigs.configStream(ServiceVendor.class).findFirst().orElseThrow().value());

        assertEquals(2, appliedConfigs.stream().count());
        assertEquals(42,
                appliedConfigs.configStream(ServiceRanking.class).findFirst().orElseThrow().value());
        assertEquals("Acme Software Foundation",
                appliedConfigs.configStream(ServiceVendor.class).findFirst().orElseThrow().value());
    }
}