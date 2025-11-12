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
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Set;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Mock implementation of {@link Configuration}.
 */
class MockConfiguration implements Configuration {

    private final String pid;
    private final String factoryPid;
    private Dictionary<String, Object> props;

    /**
     * @param pid PID
     */
    public MockConfiguration(String pid) {
        this.pid = pid;
        this.factoryPid = null;
    }

    /**
     * @param pid PID
     * @param factoryPid factory PID
     */
    public MockConfiguration(String pid, String factoryPid) {
        this.pid = pid;
        this.factoryPid = factoryPid;
    }

    @Override
    public String getPid() {
        return pid;
    }

    @Override
    public String getFactoryPid() {
        return factoryPid;
    }

    @Override
    public Dictionary<String, Object> getProperties() {
        if (props == null) {
            return null;
        }
        // return copy of dictionary
        return new Hashtable<>(MapUtil.toMap(props));
    }

    @Override
    public void update() {
        // the updating of services already registered in mock-osgi is currently not supported.
        // still allow calling this method to allow usage of {@link update(Dictionary)}, but it works
        // only if applied before registering a service in mock-osgi.
        props = newConfig(pid, factoryPid);
    }

    @Override
    public void update(Dictionary<String, ?> properties) {
        this.props = new Hashtable<>(MapUtil.toMap(properties));
        this.props.put(Constants.SERVICE_PID, pid);
        if (factoryPid != null) {
            this.props.put(ConfigurationAdmin.SERVICE_FACTORYPID, factoryPid);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean updateIfDifferent(Dictionary<String, ?> properties) throws IOException {
        if (!equals((Dictionary<String, Object>) properties, props)) {
            update(properties);
            return true;
        }
        return false;
    }

    @Override
    public void delete() {
        props = null;
    }

    @Override
    public String toString() {
        return props.toString();
    }

    private static Dictionary<String, Object> newConfig(String pid, String factoryPid) {
        Dictionary<String, Object> config = MapUtil.toDictionary(Constants.SERVICE_PID, pid);
        if (factoryPid != null) {
            config.put(ConfigurationAdmin.SERVICE_FACTORYPID, factoryPid);
        }
        return config;
    }

    // --- unsupported operations ---

    @Override
    public void setBundleLocation(String bundleLocation) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getBundleLocation() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getChangeCount() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Dictionary<String, Object> getProcessedProperties(ServiceReference<?> reference) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addAttributes(ConfigurationAttribute... attrs) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<ConfigurationAttribute> getAttributes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeAttributes(ConfigurationAttribute... attrs) throws IOException {
        throw new UnsupportedOperationException();
    }

    private static final String[] AUTO_PROPS = new String[] {
        Constants.SERVICE_PID, ConfigurationAdmin.SERVICE_FACTORYPID, ConfigurationAdmin.SERVICE_BUNDLELOCATION
    };

    // -- Comparison method for dictionaries and its helpers taken over from
    // https://github.com/apache/felix-dev/blob/3e5671ae7e5107f4f849ef9d5f0a89b1ba9d7439/configadmin/src/main/java/org/apache/felix/cm/impl/ConfigurationImpl.java#L716-L898 **/

    /**
     * Compare the two properties, ignoring auto properties
     * @param props1 Set of properties
     * @param props2 Set of properties
     * @return {@code true} if the set of properties is equal
     */
    static boolean equals(Dictionary<String, Object> props1, Dictionary<String, Object> props2) {
        if (props1 == null) {
            if (props2 == null) {
                return true;
            } else {
                return false;
            }
        } else if (props2 == null) {
            return false;
        }

        final int count1 = getCount(props1);
        final int count2 = getCount(props2);
        if (count1 != count2) {
            return false;
        }

        final Enumeration<String> keys = props1.keys();
        while (keys.hasMoreElements()) {
            final String key = keys.nextElement();
            if (!isAutoProp(key)) {
                final Object val1 = props1.get(key);
                final Object val2 = props2.get(key);
                if (val1 == null) {
                    if (val2 != null) {
                        return false;
                    }
                } else {
                    if (val2 == null) {
                        return false;
                    }
                    // arrays are compared using Arrays.equals
                    if (val1.getClass().isArray()) {
                        if (!val2.getClass().isArray()) {
                            return false;
                        }
                        final Object[] a1 = convertToObjectArray(val1);
                        final Object[] a2 = convertToObjectArray(val2);
                        if (!Arrays.equals(a1, a2)) {
                            return false;
                        }
                    } else if (!val1.equals(val2)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Convert the object to an array
     * @param value The array
     * @return an object array
     */
    private static Object[] convertToObjectArray(final Object value) {
        final Object[] values;
        if (value instanceof long[]) {
            final long[] a = (long[]) value;
            values = new Object[a.length];
            for (int i = 0; i < a.length; i++) {
                values[i] = a[i];
            }
        } else if (value instanceof int[]) {
            final int[] a = (int[]) value;
            values = new Object[a.length];
            for (int i = 0; i < a.length; i++) {
                values[i] = a[i];
            }
        } else if (value instanceof double[]) {
            final double[] a = (double[]) value;
            values = new Object[a.length];
            for (int i = 0; i < a.length; i++) {
                values[i] = a[i];
            }
        } else if (value instanceof byte[]) {
            final byte[] a = (byte[]) value;
            values = new Object[a.length];
            for (int i = 0; i < a.length; i++) {
                values[i] = a[i];
            }
        } else if (value instanceof float[]) {
            final float[] a = (float[]) value;
            values = new Object[a.length];
            for (int i = 0; i < a.length; i++) {
                values[i] = a[i];
            }
        } else if (value instanceof short[]) {
            final short[] a = (short[]) value;
            values = new Object[a.length];
            for (int i = 0; i < a.length; i++) {
                values[i] = a[i];
            }
        } else if (value instanceof boolean[]) {
            final boolean[] a = (boolean[]) value;
            values = new Object[a.length];
            for (int i = 0; i < a.length; i++) {
                values[i] = a[i];
            }
        } else if (value instanceof char[]) {
            final char[] a = (char[]) value;
            values = new Object[a.length];
            for (int i = 0; i < a.length; i++) {
                values[i] = a[i];
            }
        } else {
            values = (Object[]) value;
        }
        return values;
    }

    static boolean isAutoProp(final String name) {
        for (final String p : AUTO_PROPS) {
            if (p.equals(name)) {
                return true;
            }
        }
        return false;
    }

    static int getCount(Dictionary<String, Object> props) {
        int count = (props == null ? 0 : props.size());
        if (props != null) {
            for (final String p : AUTO_PROPS) {
                if (props.get(p) != null) {
                    count--;
                }
            }
        }
        return count;
    }
}
