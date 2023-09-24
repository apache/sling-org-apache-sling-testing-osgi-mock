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

import org.apache.sling.testing.mock.osgi.config.annotations.DynamicConfig;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.osgi.service.component.propertytypes.ServiceRanking;
import org.osgi.service.component.propertytypes.ServiceVendor;

import static org.junit.Assert.assertEquals;

@DynamicConfig(ServiceRanking.class)
@RunWith(MockitoJUnitRunner.class)
public class ConfigCollectorTest {

    @Rule
    public OsgiContext osgiContext = new OsgiContextBuilder().build();

    @Rule
    public ConfigCollector configCollector = new ConfigCollector(osgiContext,
            ServiceRanking.class, ServiceVendor.class);

    @Rule
    public ConfigCollector justRankings = new ConfigCollector(osgiContext, ServiceRanking.class);

    @DynamicConfig(ServiceVendor.class)
    @Test
    public void testEvaluate() {
        assertEquals(2, configCollector.stream().count());
        assertEquals(1, justRankings.stream().count());
    }
}
