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

import org.apache.sling.testing.mock.osgi.config.annotations.CollectConfigTypes;
import org.apache.sling.testing.mock.osgi.config.annotations.ConfigCollection;
import org.apache.sling.testing.mock.osgi.config.annotations.ConfigCollection.Entry;
import org.apache.sling.testing.mock.osgi.config.annotations.DynamicConfig;
import org.apache.sling.testing.mock.osgi.junit5.ConfigCollectionImpl.EntryImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.service.component.propertytypes.ServiceRanking;
import org.osgi.service.component.propertytypes.ServiceVendor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DynamicConfig(value = ServiceVendor.class, property = "Apache Software Foundation")
@ExtendWith(OsgiConfigParametersExtension.class)
class ConfigCollectionImplTest {

    @DynamicConfig(ServiceRanking.class)
    @Test
    void illegalEntry(ServiceRanking annotation) throws Exception {
        Constructor<?> constructor = EntryImpl.class
                .getDeclaredConstructor(Class.class, Object.class, Annotation.class);
        constructor.setAccessible(true);
        assertThrows(IllegalArgumentException.class,
                () -> {
                    try {
                        constructor.newInstance(ServiceRanking.class, "a string", annotation);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }

    @DynamicConfig(ServiceRanking.class)
    @Test
    void legalEntry(ServiceRanking config, TestInfo testInfo) {
        DynamicConfig annotation = testInfo.getTestMethod().get().getAnnotation(DynamicConfig.class);
        EntryImpl<ServiceRanking> entry = EntryImpl.of(ServiceRanking.class, config, annotation);
        assertSame(ServiceRanking.class, entry.getType());
        assertSame(config, entry.getConfig());
        assertEquals(0, entry.stream(ServiceVendor.class).count());
    }

    @SuppressWarnings("unchecked")
    @DynamicConfig(ServiceRanking.class)
    @Test
    void collectConfigTypes(@CollectConfigTypes({ServiceRanking.class}) ConfigCollection configs,
                            ServiceRanking legalConfig,
                            ServiceVendor illegalConfig) {
        ConfigCollectionImpl configsImpl = (ConfigCollectionImpl) configs;
        assertTrue(configs.stream().map(Entry::getType).anyMatch(ServiceRanking.class::isAssignableFrom));
        assertTrue(configs.stream().map(Entry::getType).noneMatch(DynamicConfig.class::isAssignableFrom));

        DynamicConfig illegalAnnotation = getClass().getAnnotation(DynamicConfig.class);
        assertThrows(IllegalArgumentException.class,
                () -> configsImpl.toEntry(illegalAnnotation));

        Entry<ServiceRanking> entry = (Entry<ServiceRanking>) configsImpl.toEntry(legalConfig);
        assertSame(ServiceRanking.class, entry.getType());
        assertSame(legalConfig, entry.getConfig());

        assertThrows(IllegalArgumentException.class, () -> configsImpl.toEntry(illegalConfig));
    }
}