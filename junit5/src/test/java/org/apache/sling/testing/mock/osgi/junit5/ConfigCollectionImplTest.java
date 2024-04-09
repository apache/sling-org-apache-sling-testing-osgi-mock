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

import org.apache.sling.testing.mock.osgi.config.annotations.ConfigCollection;
import org.apache.sling.testing.mock.osgi.config.annotations.ConfigType;
import org.apache.sling.testing.mock.osgi.config.annotations.SetConfig;
import org.apache.sling.testing.mock.osgi.config.annotations.TypedConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.service.component.propertytypes.ServiceRanking;
import org.osgi.service.component.propertytypes.ServiceVendor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SetConfig(
        pid = "common-config",
        property = {"service.ranking:Integer=42", "service.vendor=Acme Software Foundation"})
@ConfigType(type = ServiceVendor.class, property = "service.vendor=Apache Software Foundation")
@ExtendWith(OsgiConfigParametersExtension.class)
class ConfigCollectionImplTest {

    @ConfigType(type = ServiceRanking.class, lenient = true)
    @Test
    void collectConfigTypes(@CollectConfigTypes ConfigCollection configs) {
        assertTrue(configs.stream().map(TypedConfig::getType).anyMatch(ServiceRanking.class::isAssignableFrom));
        assertTrue(configs.stream().map(TypedConfig::getType).noneMatch(ConfigType.class::isAssignableFrom));
    }

    @ConfigType(type = ServiceRanking.class, property = "service.ranking:Integer=10")
    @Test
    void collectWithApplyPid(
            @CollectConfigTypes ConfigCollection unappliedConfigs,
            @CollectConfigTypes(pid = "common-config") ConfigCollection appliedConfigs) {
        assertEquals(2, unappliedConfigs.stream().count()); // excludes @Test and @ExtendWith
        assertEquals(
                10,
                unappliedConfigs
                        .configStream(ServiceRanking.class)
                        .findFirst()
                        .orElseThrow()
                        .value());
        assertEquals(
                "Apache Software Foundation",
                unappliedConfigs
                        .configStream(ServiceVendor.class)
                        .findFirst()
                        .orElseThrow()
                        .value());

        assertEquals(2, appliedConfigs.stream().count()); // excludes @Test and @ExtendWith
        assertEquals(
                42,
                appliedConfigs
                        .configStream(ServiceRanking.class)
                        .findFirst()
                        .orElseThrow()
                        .value());
        assertEquals(
                "Acme Software Foundation",
                appliedConfigs
                        .configStream(ServiceVendor.class)
                        .findFirst()
                        .orElseThrow()
                        .value());
    }
}
