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

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.function.Consumer;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

/**
 * JUnit 5 extension that allows to inject {@link OsgiContext} (or subclasses of
 * it) parameters in test methods, and ensures that the context is set up and
 * teared down properly for each test method.
 */
public final class OsgiContextExtension implements ParameterResolver, TestInstancePostProcessor, BeforeEachCallback,
        AfterEachCallback, AfterTestExecutionCallback {

    /**
     * Checks if test class has a {@link OsgiContext} or derived field. If it has
     * and is not instantiated, create an new {@link OsgiContext} and store it in
     * the field. If it is already instantiated reuse this instance and use it
     * for all test methods.
     */
    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext extensionContext) throws Exception {
        Field osgiContextField = getFieldFromTestInstance(testInstance, OsgiContext.class);
        if (osgiContextField != null) {
            OsgiContext context = (OsgiContext)osgiContextField.get(testInstance);
            if (context != null) {
                if (!context.isSetUp()) {
                    context.setUpContext();
                }
                OsgiContextStore.storeOsgiContext(extensionContext, testInstance, context);
            } else {
                context = OsgiContextStore.getOrCreateOsgiContext(extensionContext, testInstance);
                osgiContextField.set(testInstance, context);
            }
        }
    }

    /**
     * Support parameter injection for test methods of parameter type is derived from {@link OsgiContext}.
     */
    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return OsgiContext.class.isAssignableFrom(parameterContext.getParameter().getType());
    }

    /**
     * Resolve (or create) {@link OsgiContext} instance for test method parameter.
     */
    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return OsgiContextStore.getOrCreateOsgiContext(extensionContext, extensionContext.getRequiredTestInstance());
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        applyOsgiContext(extensionContext, osgiContext -> {
            // call context plugins setup after all @BeforeEach methods were called
            osgiContext.getContextPlugins().executeAfterSetUpCallback(osgiContext);
        });
    }

    @Override
    public void afterTestExecution(ExtensionContext extensionContext) throws Exception {
        applyOsgiContext(extensionContext, osgiContext -> {
            // call context plugins setup before any @AfterEach method is called
            osgiContext.getContextPlugins().executeBeforeTearDownCallback(osgiContext);
        });
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) {
        applyOsgiContext(extensionContext, osgiContext -> {
            // call context plugins setup after all @AfterEach methods were called
            osgiContext.getContextPlugins().executeAfterTearDownCallback(osgiContext);

            // Tear down {@link OsgiContext} after test is complete.
            osgiContext.tearDownContext();
            OsgiContextStore.removeOsgiContext(extensionContext, extensionContext.getRequiredTestInstance());
        });
    }

    private void applyOsgiContext(ExtensionContext extensionContext, Consumer<OsgiContext> consumer) {
        OsgiContext osgiContext = OsgiContextStore.getOsgiContext(extensionContext,
                extensionContext.getRequiredTestInstance());
        if (osgiContext != null) {
            consumer.accept(osgiContext);
        }
    }

    private Field getFieldFromTestInstance(Object testInstance, Class<?> type) {
        return getFieldFromTestInstance(testInstance.getClass(), type);
    }

    private Field getFieldFromTestInstance(Class<?> instanceClass, Class<?> type) {
        if (instanceClass == null) {
            return null;
        }
        Field contextField = Arrays.stream(instanceClass.getDeclaredFields())
                .filter(field -> type.isAssignableFrom(field.getType())).findFirst().orElse(null);
        if (contextField != null) {
            contextField.setAccessible(true);
        } else {
            return getFieldFromTestInstance(instanceClass.getSuperclass(), type);
        }
        return contextField;
    }

}
