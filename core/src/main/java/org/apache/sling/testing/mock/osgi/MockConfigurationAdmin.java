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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Mock implementation of {@link ConfigurationAdmin}.
 */
class MockConfigurationAdmin implements ConfigurationAdmin {

    private final BundleContext bundleContext;
    private final ConcurrentMap<String, Configuration> configs = new ConcurrentHashMap<>();

    MockConfigurationAdmin(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    public Configuration getConfiguration(final String pid) throws IOException {
        configs.putIfAbsent(pid, new MockConfiguration(pid));
        return configs.get(pid);
    }

    @Override
    public Configuration getConfiguration(final String pid, final String location) throws IOException {
        return getConfiguration(pid);
    }

    @Override
    @SuppressWarnings("squid:S1168")
    public Configuration[] listConfigurations(final String filter) throws IOException, InvalidSyntaxException {
        final Filter filterObject = bundleContext.createFilter(filter);
        final Configuration[] filtered = configs.values().stream()
                .filter(configuration -> filterObject.match(configuration.getProperties()))
                .toArray(Configuration[]::new);
        if (filtered.length != 0) {
            return filtered;
        }
        return null;
    }

    @Override
    public Configuration getFactoryConfiguration(final String factoryPid, final String name, final String location)
            throws IOException {
        return getFactoryConfiguration(factoryPid, name);
    }

    @Override
    public Configuration getFactoryConfiguration(final String factoryPid, final String name) throws IOException {
        String pid = String.format("%s~%s", factoryPid, name);
        configs.putIfAbsent(pid, new MockConfiguration(pid, factoryPid));
        return configs.get(pid);
    }

    // --- unsupported operations ---

    @Override
    public Configuration createFactoryConfiguration(final String factoryPid) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Configuration createFactoryConfiguration(final String factoryPid, final String location) throws IOException {
        throw new UnsupportedOperationException();
    }
}
