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
package org.apache.sling.testing.mock.osgi.config.annotations;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.mockito.quality.Strictness;
import org.osgi.service.component.propertytypes.ServiceRanking;
import org.osgi.service.component.propertytypes.ServiceVendor;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

public class ConfigCollectionTest {

    @SuppressWarnings({"unchecked", "null"})
    private <T> TypedConfig<T> newMockEntry(@NotNull final Class<T> configType, @NotNull final T config) {
        TypedConfig<T> mocked = mock(TypedConfig.class, withSettings().strictness(Strictness.LENIENT));
        doReturn(configType).when(mocked).getType();
        doReturn(config).when(mocked).getConfig();
        doCallRealMethod().when(mocked).stream(any(Class.class));
        doCallRealMethod().when(mocked).configStream(any(Class.class));
        return mocked;
    }

    @SuppressWarnings({"unchecked", "null"})
    private ConfigCollection newMockConfigCollection(
            @NotNull final Supplier<Stream<TypedConfig<?>>> entryStreamSupplier) {
        ConfigCollection mocked = mock(ConfigCollection.class, withSettings().strictness(Strictness.LENIENT));
        doCallRealMethod().when(mocked).stream(any(Class.class));
        doCallRealMethod().when(mocked).configStream(any(Class.class));
        doAnswer(call -> entryStreamSupplier.get()).when(mocked).stream();
        return mocked;
    }

    @Test
    public void entryStream() {
        ServiceRanking serviceRanking = mock(ServiceRanking.class);
        ServiceVendor serviceVendor = mock(ServiceVendor.class);

        TypedConfig<ServiceRanking> serviceRankingEntry = newMockEntry(ServiceRanking.class, serviceRanking);
        assertSame(
                serviceRankingEntry,
                serviceRankingEntry.stream(ServiceRanking.class).findFirst().orElseThrow());
        assertEquals(1, serviceRankingEntry.stream(ServiceRanking.class).count());
        assertEquals(0, serviceRankingEntry.stream(ServiceVendor.class).count());
        assertSame(
                serviceRanking,
                serviceRankingEntry
                        .configStream(ServiceRanking.class)
                        .findFirst()
                        .orElseThrow());
        assertEquals(1, serviceRankingEntry.configStream(ServiceRanking.class).count());
        assertEquals(0, serviceRankingEntry.configStream(ServiceVendor.class).count());

        TypedConfig<ServiceVendor> serviceVendorEntry = newMockEntry(ServiceVendor.class, serviceVendor);
        assertSame(
                serviceVendorEntry,
                serviceVendorEntry.stream(ServiceVendor.class).findFirst().orElseThrow());
        assertEquals(1, serviceVendorEntry.stream(ServiceVendor.class).count());
        assertEquals(0, serviceVendorEntry.stream(ServiceRanking.class).count());
        assertSame(
                serviceVendor,
                serviceVendorEntry.configStream(ServiceVendor.class).findFirst().orElseThrow());
        assertEquals(1, serviceVendorEntry.configStream(ServiceVendor.class).count());
        assertEquals(0, serviceVendorEntry.configStream(ServiceRanking.class).count());

        final List<TypedConfig<?>> entries = List.of(serviceRankingEntry, serviceVendorEntry);

        ConfigCollection configCollection = newMockConfigCollection(entries::stream);
        assertArrayEquals(
                new TypedConfig<?>[] {serviceRankingEntry},
                configCollection.stream(ServiceRanking.class).toArray());
        assertArrayEquals(
                new TypedConfig<?>[] {serviceVendorEntry},
                configCollection.stream(ServiceVendor.class).toArray());
        assertArrayEquals(
                new ServiceRanking[] {serviceRanking},
                configCollection.configStream(ServiceRanking.class).toArray());
        assertArrayEquals(
                new ServiceVendor[] {serviceVendor},
                configCollection.configStream(ServiceVendor.class).toArray());
    }
}
