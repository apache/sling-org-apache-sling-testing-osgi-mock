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
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.sling.testing.mock.osgi.OsgiMetadataUtil.OsgiMetadata;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentConstants;

import static org.apache.sling.testing.mock.osgi.MapUtil.toDictionary;
import static org.apache.sling.testing.mock.osgi.MapUtil.toMap;

/**
 * Map util merge methods.
 */
final class MapMergeUtil {

    static final AtomicLong COMPONENT_ID_COUNTER = new AtomicLong();

    private MapMergeUtil() {
        // static methods only
    }

    /**
     * Merge DS component properties from the following sources (with this precedence):
     * 1. Automatically generated DS service properties
     * 2. Properties defined in calling unit test code
     * 3. Properties from ConfigurationAdmin
     * 4. Properties from the OSGi DS Component Description (in the context of Apache Felix referred to as SCR metadata)
     * @param targetClass Target service class
     * @param configAdmin Configuration admin or null if none is registered
     * @param properties Properties from unit test code or null if none where passed
     * @return Merged properties
     * @see <a href="http://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.component.html#service.component-component.properties">Component Properties</a>
     */
    static Dictionary<String, Object> propertiesMergeWithOsgiMetadata(
            Class<?> targetClass, ConfigurationAdmin configAdmin, Dictionary<String, Object> properties) {
        return toDictionary(propertiesMergeWithOsgiMetadata(targetClass, configAdmin, toMap(properties)));
    }

    /**
     * Merge DS component properties relevant for DS components from the following sources (with this precedence):
     * 1. Automatically generated DS service properties
     * 2. Properties defined in calling unit test code
     * 3. Properties from ConfigurationAdmin
     * 4. Properties from the OSGi DS Component Description (in the context of Apache Felix referred to as SCR metadata)
     * @param targetClass Target service class
     * @param configAdmin Configuration admin or null if none is registered
     * @param properties Properties from unit test code or null if none where passed
     * @return Merged properties
     * @see <a href="http://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.component.html#service.component-component.properties">Component Properties</a>
     */
    static Map<String, Object> propertiesMergeWithOsgiMetadata(
            Class<?> targetClass, ConfigurationAdmin configAdmin, Map<String, Object> properties) {
        Map<String, Object> mergedProperties = new HashMap<>();

        OsgiMetadata metadata = OsgiMetadataUtil.getMetadata(targetClass);
        if (metadata != null) {
            Map<String, Object> metadataProperties = metadata.getProperties();
            if (metadataProperties != null) {
                mergedProperties.putAll(metadataProperties);

                // merge with configuration from config admin
                if (configAdmin != null) {
                    for (String pid : metadata.getConfigurationPID()) {
                        if (pid != null) {
                            try {
                                Configuration config = configAdmin.getConfiguration(pid);
                                Dictionary<String, Object> caProperties = config.getProperties();
                                if (caProperties != null) {
                                    mergedProperties.putAll(toMap(caProperties));
                                }
                            } catch (IOException ex) {
                                throw new RuntimeException("Unable to read config for pid " + pid, ex);
                            }
                        }
                    }
                }
            }
        }

        // merge with properties from calling unit test code
        if (properties != null) {
            mergedProperties.putAll(properties);
        }

        // add non overwritable auto-generated properties
        mergedProperties.put(ComponentConstants.COMPONENT_NAME, targetClass.getName());
        mergedProperties.put(ComponentConstants.COMPONENT_ID, COMPONENT_ID_COUNTER.getAndIncrement());
        return mergedProperties;
    }
}
