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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.impl.inject.Annotations;
import org.apache.sling.testing.mock.osgi.OsgiMetadataUtil.DynamicReference;
import org.apache.sling.testing.mock.osgi.OsgiMetadataUtil.FieldCollectionType;
import org.apache.sling.testing.mock.osgi.OsgiMetadataUtil.OsgiMetadata;
import org.apache.sling.testing.mock.osgi.OsgiMetadataUtil.Reference;
import org.apache.sling.testing.mock.osgi.OsgiMetadataUtil.ReferencePolicy;
import org.apache.sling.testing.mock.osgi.OsgiMetadataUtil.ReferencePolicyOption;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentServiceObjects;

/**
 * Helper methods to inject dependencies and activate services.
 */
final class OsgiServiceUtil {

    private OsgiServiceUtil() {
        // static methods only
    }

    /**
     * Simulate activation or deactivation of OSGi service instance.
     * @param target Service instance.
     * @param componentContext Component context
     * @return true if activation/deactivation method was called. False if it failed.
     */
    public static boolean activateDeactivate(Object target, MockComponentContext componentContext, boolean activate) {
        Class<?> targetClass = target.getClass();

        // get method name for activation/deactivation from osgi metadata
        OsgiMetadata metadata = OsgiMetadataUtil.getMetadata(targetClass);
        if (metadata == null) {
            throw new NoScrMetadataException(targetClass);
        }
        String methodName;
        if (activate) {
            methodName = metadata.getActivateMethodName();
        } else {
            methodName = metadata.getDeactivateMethodName();
        }
        boolean fallbackDefaultName = false;
        if (StringUtils.isEmpty(methodName)) {
            fallbackDefaultName = true;
            if (activate) {
                methodName = "activate";
            } else {
                methodName = "deactivate";
            }
        }

        // try to find matching activate/deactivate method and execute it
        if (invokeLifecycleMethod(target, targetClass, methodName, !activate,
                componentContext, componentContext.getPropertiesAsMap())) {
            return true;
        }

        if (fallbackDefaultName) {
            return false;
        }

        throw new RuntimeException("No matching " + (activate ? "activation" : "deactivation") + " method with name '" + methodName + "' "
                + " found in class " + targetClass.getName());
    }

    /**
     * Simulate modification of configuration of OSGi service instance.
     * @param target Service instance.
     * @param properties Updated configuration
     * @return true if modified method was called. False if it failed.
     */
    public static boolean modified(Object target, MockComponentContext componentContext, Map<String,Object> properties) {
        Class<?> targetClass = target.getClass();

        // get method name for activation/deactivation from osgi metadata
        OsgiMetadata metadata = OsgiMetadataUtil.getMetadata(targetClass);
        if (metadata == null) {
            throw new NoScrMetadataException(targetClass);
        }
        String methodName = metadata.getModifiedMethodName();
        if (StringUtils.isEmpty(methodName)) {
            return false;
        }

        // try to find matching modified method and execute it
        if (invokeLifecycleMethod(target, targetClass, methodName, false, componentContext, properties)) {
            return true;
        }

        throw new RuntimeException("No matching modified method with name '" + methodName + "' "
                + " found in class " + targetClass.getName());
    }

    /**
     * Invokes a lifecycle method (activation, deactivation or modified) with variable method arguments.
     * @param target Target object
     * @param targetClass Target object class
     * @param methodName Method name
     * @param allowIntegerArgument Allow int or Integer as arguments (only decactivate)
     * @param componentContext Component context
     * @param properties Component properties
     * @return true if a method was found and invoked
     */
    private static boolean invokeLifecycleMethod(Object target, Class<?> targetClass,
            String methodName, boolean allowIntegerArgument,
            MockComponentContext componentContext, Map<String,Object> properties) {

        // 1. componentContext
        Method method = getMethod(targetClass, methodName, new Class<?>[] { ComponentContext.class });
        if (method != null) {
            invokeMethod(target, method, new Object[] { componentContext });
            return true;
        }

        // 2. bundleContext
        method = getMethod(targetClass, methodName, new Class<?>[] { BundleContext.class });
        if (method != null) {
            invokeMethod(target, method, new Object[] { componentContext.getBundleContext() });
            return true;
        }

        // 3. map
        method = getMethod(targetClass, methodName, new Class<?>[] { Map.class });
        if (method != null) {
            invokeMethod(target, method, new Object[] { componentContext.getPropertiesAsMap() });
            return true;
        }

        // 4. Component property type (annotation lass)
        method = getMethod(targetClass, methodName, new Class<?>[] { Annotation.class });
        if (method != null) {
            invokeMethod(target, method, new Object[] { Annotations.toObject(method.getParameterTypes()[0],
                    componentContext.getPropertiesAsMap(),
                    componentContext.getBundleContext().getBundle(), false) });
            return true;
        }

        // 5. int (deactivation only)
        if (allowIntegerArgument) {
            method = getMethod(targetClass, methodName, new Class<?>[] { int.class });
            if (method != null) {
                invokeMethod(target, method, new Object[] { 0 });
                return true;
            }
        }

        // 6. Integer (deactivation only)
        if (allowIntegerArgument) {
            method = getMethod(targetClass, methodName, new Class<?>[] { Integer.class });
            if (method != null) {
                invokeMethod(target, method, new Object[] { 0 });
                return true;
            }
        }

        // 7. mixed arguments
        Class<?>[] mixedArgsAllowed = allowIntegerArgument ?
                new Class<?>[] { ComponentContext.class, BundleContext.class, Map.class, Annotation.class, int.class, Integer.class }
                : new Class<?>[] { ComponentContext.class, BundleContext.class, Map.class, Annotation.class };
        method = getMethodWithAnyCombinationArgs(targetClass, methodName, mixedArgsAllowed);
        if (method != null) {
            Object[] args = new Object[method.getParameterTypes().length];
            for (int i=0; i<args.length; i++) {
                if (method.getParameterTypes()[i] == ComponentContext.class) {
                    args[i] = componentContext;
                }
                else if (method.getParameterTypes()[i] == BundleContext.class) {
                    args[i] = componentContext.getBundleContext();
                }
                else if (method.getParameterTypes()[i] == Map.class) {
                    args[i] = componentContext.getPropertiesAsMap();
                }
                else if (method.getParameterTypes()[i].isAnnotation()) {
                    args[i] = Annotations.toObject(method.getParameterTypes()[i],
                            componentContext.getPropertiesAsMap(),
                            componentContext.getBundleContext().getBundle(), false);
                }
                else if (method.getParameterTypes()[i] == int.class || method.getParameterTypes()[i] == Integer.class) {
                    args[i] = 0;
                }
            }
            invokeMethod(target, method, args);
            return true;
        }

        // 8. noargs
        method = getMethod(targetClass, methodName, new Class<?>[0]);
        if (method != null) {
            invokeMethod(target, method, new Object[0]);
            return true;
        }

        return false;
    }

    private static Method getMethod(Class clazz, String methodName, Class<?>[] types) {
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (StringUtils.equals(method.getName(), methodName) && method.getParameterTypes().length==types.length) {
                boolean foundMismatch = false;
                for (int i=0; i<types.length; i++) {
                    if (!((method.getParameterTypes()[i]==types[i])
                            || (types[i]==Annotation.class && method.getParameterTypes()[i].isAnnotation()))) {
                        foundMismatch = true;
                        break;
                    }
                }
                if (!foundMismatch) {
                    return method;
                }
            }
        }
        // not found? check super classes
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            return getMethod(superClass, methodName, types);
        }
        return null;
    }

    private static Method getMethodWithAssignableTypes(Class clazz, String methodName, Class<?>[] types) {
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (StringUtils.equals(method.getName(), methodName) && method.getParameterTypes().length==types.length) {
                boolean foundMismatch = false;
                for (int i=0; i<types.length; i++) {
                    if (!method.getParameterTypes()[i].isAssignableFrom(types[i])) {
                        foundMismatch = true;
                        break;
                    }
                }
                if (!foundMismatch) {
                    return method;
                }
            }
        }
        // not found? check super classes
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            return getMethodWithAssignableTypes(superClass, methodName, types);
        }
        return null;
    }

    private static Method getMethodWithAnyCombinationArgs(Class clazz, String methodName, Class<?>[] types) {
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (StringUtils.equals(method.getName(), methodName) && method.getParameterTypes().length > 1) {
                boolean foundMismatch = false;
                for (Class<?> parameterType : method.getParameterTypes()) {
                    boolean foundAnyMatch = false;
                    for (int i=0; i<types.length; i++) {
                        if (types[i] == Annotation.class) {
                            if (parameterType.isAnnotation()) {
                                foundAnyMatch = true;
                                break;
                            }
                        }
                        else if (types[i] == ComponentContext.class || types[i] == BundleContext.class
                                || types[i] == ServiceReference.class || types[i] == ComponentServiceObjects.class
                                || types[i] == Map.class || types[i] == int.class || types[i] == Integer.class) {
                            if (parameterType == types[i]) {
                                foundAnyMatch = true;
                                break;
                            }
                        }
                        else if (parameterType.isAssignableFrom(types[i])) {
                            foundAnyMatch = true;
                            break;
                        }
                    }
                    if (!foundAnyMatch) {
                        foundMismatch = true;
                        break;
                    }
                }
                if (!foundMismatch) {
                    return method;
                }
            }
        }
        // not found? check super classes
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            return getMethodWithAnyCombinationArgs(superClass, methodName, types);
        }
        return null;
    }

    private static void invokeMethod(Object target, Method method, Object[] args) {
        try {
            method.setAccessible(true);
            method.invoke(target, args);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException("Unable to invoke method '" + method.getName() + "' for class "
                    + target.getClass().getName(), ex);
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Unable to invoke method '" + method.getName() + "' for class "
                    + target.getClass().getName(), ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException("Unable to invoke method '" + method.getName() + "' for class "
                    + target.getClass().getName(), ex.getCause());
        }
    }

    private static Field getField(Class clazz, String fieldName, Class<?> type) {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (StringUtils.equals(field.getName(), fieldName) && field.getType().equals(type)) {
                return field;
            }
        }
        // not found? check super classes
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            return getField(superClass, fieldName, type);
        }
        return null;
    }

    private static Field getFieldWithAssignableType(Class clazz, String fieldName, Class<?> type) {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (StringUtils.equals(field.getName(), fieldName) && field.getType().isAssignableFrom(type)) {
                return field;
            }
        }
        // not found? check super classes
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            return getFieldWithAssignableType(superClass, fieldName, type);
        }
        return null;
    }

    private static Field getCollectionField(Class clazz, String fieldName) {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (StringUtils.equals(field.getName(), fieldName) && Collection.class.isAssignableFrom(field.getType())) {
                return field;
            }
        }
        // not found? check super classes
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            return getCollectionField(superClass, fieldName);
        }
        return null;
    }

    private static void setField(Object target, Field field, Object value) {
        try {
            field.setAccessible(true);
            field.set(target, value);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException("Unable to set field '" + field.getName() + "' for class "
                    + target.getClass().getName(), ex);
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Unable to set field '" + field.getName() + "' for class "
                    + target.getClass().getName(), ex);
        }
    }

    /**
     * Simulate OSGi service dependency injection. Injects direct references and multiple references.
     * @param target Service instance
     * @param bundleContext Bundle context from which services are fetched to inject.
     * @param properties Services properties (used to resolve dynamic reference properties)
     * @return true if all dependencies could be injected, false if the service has no dependencies.
     */
    public static boolean injectServices(Object target, BundleContext bundleContext, Map<String, Object> properties) {

        // collect all declared reference annotations on class and field level
        Class<?> targetClass = target.getClass();

        OsgiMetadata metadata = OsgiMetadataUtil.getMetadata(targetClass);
        if (metadata == null) {
            throw new NoScrMetadataException(targetClass);
        }

        // try to inject services
        boolean foundAny = false;
        for (Reference reference : metadata.getReferences()) {
            if (reference.isConstructorParameter()) {
                continue;
            }
            if (properties != null) {
                // Look for a target override
                Object o = properties.get(reference.getName() + ".target");
                if (o instanceof String) {
                    reference = new DynamicReference(reference,(String)o);
                }
            }
            injectServiceReference(reference, target, bundleContext);
            foundAny = true;
        }
        return foundAny;
    }

    /**
     * Simulate OSGi service dependency injection. Injects direct references and multiple references.
     * @param targetClass Service class
     * @param componentContext Component context
     * @return true if all dependencies could be injected, false if the service has no dependencies.
     */
    @SuppressWarnings("null")
    public static @NotNull <T> T activateInjectServices(Class<T> targetClass, MockComponentContext componentContext) {
        T target;
        try {
            // try to find constructor with parameter matching the OSGi metadata
            target = instantiateServiceWithActivateInject(targetClass, componentContext);
            // fallback to default constructor
            if (target == null) {
                target = targetClass.newInstance();
            }
        }
        catch (IllegalAccessException | InstantiationException | InvocationTargetException ex) {
            throw new RuntimeException("Error creating instance of " + targetClass.getName() + ": " + ex.getMessage(), ex);
        }

        // check if there are additional references outside constructor, inject them as well
        injectServices(target, componentContext.getBundleContext(), componentContext.getPropertiesAsMap());

        // check for dedicated activate method
        activateDeactivate(target, componentContext, true);

        return target;
    }

    @SuppressWarnings("unchecked")
    private static @Nullable <T> T instantiateServiceWithActivateInject(Class<T> targetClass, MockComponentContext componentContext)
            throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        OsgiMetadata metadata = OsgiMetadataUtil.getMetadata(targetClass);
        if (metadata == null) {
            return null;
        }

        // get list of constructor injection references, ordered by parameter number
        List<Reference> constructorInjectionReferences = metadata.getReferences().stream()
                .filter(Reference::isConstructorParameter)
                .sorted((ref1, ref2) -> ref1.getParameter().compareTo(ref2.getParameter()))
                .collect(Collectors.toList());

        // go through all constructors and try to find a matching one
        Constructor<T> matchingConstructor = null;
        List<Object> constructorParamValues = null;
        for (Constructor<T> constructor : (Constructor<T>[])targetClass.getConstructors()) {
            Optional<List<Object>> values = buildConstructorInjectionValues(targetClass, constructor,
                    componentContext, constructorInjectionReferences);
            if (values.isPresent()) {
                matchingConstructor = constructor;
                constructorParamValues = values.get();
                break;
            }
        }
        if (matchingConstructor != null && constructorParamValues != null) {
            return matchingConstructor.newInstance(constructorParamValues.toArray(new Object[0]));
        }
        else {
            return null;
        }
    }

    private static <T> Optional<List<Object>> buildConstructorInjectionValues(Class<T> targetClass, Constructor<T> constructor,
            MockComponentContext componentContext, List<Reference> constructorInjectionReferences)
            throws InstantiationException, IllegalAccessException {
        Iterator<Reference> referenceIterator = constructorInjectionReferences.iterator();
        List<Object> values = new ArrayList<>();
        int parameterIndex = 0;
        for (Parameter parameter : constructor.getParameters()) {
            // check for well-known parameter types first
            if (parameter.getType() == ComponentContext.class) {
                values.add(componentContext);
            }
            else if (parameter.getType() == BundleContext.class) {
                values.add(componentContext.getBundleContext());
            }
            else if (parameter.getType() == Map.class) {
                values.add(componentContext.getPropertiesAsMap());
            }
            else if (parameter.getType().isAnnotation()) {
                values.add(Annotations.toObject(parameter.getType(),
                        componentContext.getPropertiesAsMap(),
                        componentContext.getBundleContext().getBundle(), false));
            }
            // check for reference injection
            else if (referenceIterator.hasNext()) {
                Reference reference = referenceIterator.next();
                Optional<?> referenceValue = buildConstructorInjectionValue(targetClass, parameter.getType(), reference, componentContext, parameterIndex++);
                if (referenceValue != null) {
                    values.add(referenceValue.isPresent() ? referenceValue.get() : null);
                }
                else {
                    // reference not found, constructor is invalid
                    return Optional.empty();
                }
            }
            else {
                // parameter does not match, constructor is invalid
                return Optional.empty();
            }
        }
        return Optional.of(values);
    }

    /**
     * Build value to be injected in constructor parameter.
     * @param <T> Parameter type
     * @param targetClass Target class containing the constructor
     * @param parameterType Parameter type
     * @param reference Reference
     * @param componentContext Component context
     * @param parameterIndex 0-based index of the constructor parameter for which the injection value is calculated
     * @return null if parameter could not be injected, empty Optional if null value should be injected, or value wrapped in Optional otherwise
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    private static <T> @Nullable Optional<?> buildConstructorInjectionValue(Class<?> targetClass, Class<T> parameterType, Reference reference,
            MockComponentContext componentContext, int parameterIndex) throws InstantiationException, IllegalAccessException {
        Class<?> type = reference.getInterfaceTypeAsClass();
        // get matching service references
        List<ServiceInfo<?>> matchingServices = getMatchingServices(type,
                componentContext.getBundleContext(), reference.getTarget());

        if (matchingServices.isEmpty() && !reference.isCardinalityOptional()) {
            throw new ReferenceViolationException("Unable to inject mandatory reference '" + reference.getName() + "' (" + type.getName() +  ") into constructor parameter " + parameterIndex + " for class " + targetClass.getName() + " : no matching services were found.");
        }

        // check for field with list/collection reference
        if (reference.isCardinalityMultiple()) {
            Collection<Object> collection = newCollectionInstance(parameterType);
            switch (reference.getFieldCollectionType()) {
                case SERVICE:
                    matchingServices.stream()
                        .map(ServiceInfo::getServiceInstance)
                        .forEach(collection::add);
                    break;
                case REFERENCE:
                    matchingServices.stream()
                        .map(ServiceInfo::getServiceReference)
                        .forEach(collection::add);
                    break;
                default:
                    throw new RuntimeException("Field collection type '" + reference.getFieldCollectionType() + "' not supported "
                            + "for reference '" + reference.getName()  + "' (" + type.getName() +  ") into constructor parameter " + parameterIndex + " for class " +  targetClass.getName());
            }
            return Optional.of(collection);
        }

        // check for single field reference
        else {
            Optional<ServiceInfo<?>> firstServiceInfo = matchingServices.stream().findFirst();

            // 1. assignable from service instance
            if (parameterType.isAssignableFrom(reference.getInterfaceTypeAsClass())) {
                return firstServiceInfo.map(ServiceInfo::getServiceInstance);
            }

            // 2. ServiceReference
            if (parameterType == ServiceReference.class) {
                return firstServiceInfo.map(ServiceInfo::getServiceReference);
            }
        }

        // no match
        return null;
    }

    private static void injectServiceReference(Reference reference, Object target, BundleContext bundleContext) {
        Class<?> targetClass = target.getClass();

        // get reference type
        Class<?> type = reference.getInterfaceTypeAsClass();

        // get matching service references
        List<ServiceInfo<?>> matchingServices = getMatchingServices(type, bundleContext, reference.getTarget());

        // no references found? check if reference was optional
        if (matchingServices.isEmpty()) {
            if (!reference.isCardinalityOptional()) {
                throw new ReferenceViolationException("Unable to inject mandatory reference '" + reference.getName() + "' (" + type.getName() +  ") for class " + targetClass.getName() + " : no matching services were found.");
            }
            if (reference.isCardinalityMultiple()) {
                // make sure at least empty array is set
                invokeBindUnbindMethod(reference, target, null, bundleContext, true);
            }
        }

        // multiple references found? inject only first one with highest ranking
        if (matchingServices.size() > 1 && !reference.isCardinalityMultiple()) {
            matchingServices = matchingServices.subList(0, 1);
        } else {
            /*
             * Please note that the OSGi spec does not seem to define a ordering for the list of service references/services
             * https://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.component.html#service.component-field.injection
             * But the actual Felix framework implementation seems to return the list always sorted by rank in ascending order, so we do the same here.
             */
            matchingServices.sort(Comparator.comparing(ServiceInfo::getServiceReference));
        }


        // try to invoke bind method
        for (ServiceInfo<?> matchingService : matchingServices) {
            invokeBindUnbindMethod(reference, target, matchingService, bundleContext, true);
        }
    }

    private static void invokeBindUnbindMethod(Reference reference, Object target, ServiceInfo<?> serviceInfo, BundleContext bundleContext, boolean bind) {
        Class<?> targetClass = target.getClass();

        // try to invoke bind method
        String methodName = bind ? reference.getBind() : reference.getUnbind();
        String fieldName = reference.getField();

        if (StringUtils.isEmpty(methodName) && StringUtils.isEmpty(fieldName)) {
            throw new RuntimeException("No bind/unbind method name or file name defined "
                    + "for reference '" + reference.getName() + "' (" + reference.getInterfaceTypeAsClass().getName() +  ") for class " +  targetClass.getName());
        }

        if (StringUtils.isNotEmpty(methodName) && serviceInfo != null) {

            // 1. ServiceReference
            Method method = getMethod(targetClass, methodName, new Class<?>[] { ServiceReference.class });
            if (method != null) {
                invokeMethod(target, method, new Object[] { serviceInfo.getServiceReference() });
                return;
            }

            // 2. ComponentServiceObjects
            method = getMethod(targetClass, methodName, new Class<?>[] { ComponentServiceObjects.class });
            if (method != null) {
                invokeMethod(target, method, new Object[] { toComponentServiceObjects(bundleContext.getServiceObjects(serviceInfo.getServiceReference())) });
                return;
            }

            // 3. assignable from service instance
            Class<?> interfaceType = reference.getInterfaceTypeAsClass();
            method = getMethodWithAssignableTypes(targetClass, methodName, new Class<?>[] { interfaceType });
            if (method != null) {
                invokeMethod(target, method, new Object[] { serviceInfo.getServiceInstance() });
                return;
            }

            // 4. Map
            method = getMethod(targetClass, methodName, new Class<?>[] { Map.class });
            if (method != null) {
                invokeMethod(target, method, new Object[] { serviceInfo.getServiceConfig() });
                return;
            }

            // 5. mixed arguments
            Class<?>[] mixedArgsAllowed = new Class<?>[] { ServiceReference.class, ComponentServiceObjects.class, interfaceType, Map.class };
            method = getMethodWithAnyCombinationArgs(targetClass, methodName, mixedArgsAllowed);
            if (method != null) {
                Object[] args = new Object[method.getParameterTypes().length];
                for (int i=0; i<args.length; i++) {
                    if (method.getParameterTypes()[i] == ServiceReference.class) {
                        args[i] = serviceInfo.getServiceReference();
                    }
                    else if (method.getParameterTypes()[i] == ComponentServiceObjects.class) {
                        args[i] = toComponentServiceObjects(bundleContext.getServiceObjects(serviceInfo.getServiceReference()));
                    }
                    else if (method.getParameterTypes()[i].isAssignableFrom(interfaceType)) {
                        args[i] = serviceInfo.getServiceInstance();
                    }
                    else if (method.getParameterTypes()[i] == Map.class) {
                        args[i] = serviceInfo.getServiceConfig();
                    }
                }
                invokeMethod(target, method, args);
                return;
            }

            throw new RuntimeException((bind ? "Bind" : "Unbind") + " method with name " + methodName + " not found "
                    + "for reference '" + reference.getName() + "' for class " +  targetClass.getName());
        }

        // OSGi declarative services 1.3 supports modifying the field directly
        else if (StringUtils.isNotEmpty(fieldName)) {

            // check for field with list/collection reference
            if (reference.isCardinalityMultiple()) {
                switch (reference.getFieldCollectionType()) {
                    case SERVICE:
                    case REFERENCE:
                        Object item = null;
                        if (serviceInfo != null) {
                            item = serviceInfo.getServiceInstance();
                            if (reference.getFieldCollectionType() == FieldCollectionType.REFERENCE) {
                                item = serviceInfo.getServiceReference();
                            }
                        }
                        Field field = getCollectionField(targetClass, fieldName);
                        if (field != null) {
                            if (bind) {
                                addToCollection(target, field, item);
                            }
                            else {
                                removeFromCollection(target, field, item);
                            }
                            return;
                        }
                        break;
                    default:
                        throw new RuntimeException("Field collection type '" + reference.getFieldCollectionType() + "' not supported "
                                + "for reference '" + reference.getName() + "' (" + reference.getInterfaceTypeAsClass().getName() +  ") for class " +  targetClass.getName());
                }
            }

            // check for single field reference
            else {
                // 1. assignable from service instance
                Class<?> interfaceType = reference.getInterfaceTypeAsClass();
                Field field = getFieldWithAssignableType(targetClass, fieldName, interfaceType);
                if (field != null) {
                    setField(target, field, bind && serviceInfo != null ? serviceInfo.getServiceInstance() : null);
                    return;
                }

                // 2. ServiceReference
                field = getField(targetClass, fieldName, ServiceReference.class);
                if (field != null) {
                    setField(target, field, bind && serviceInfo != null ? serviceInfo.getServiceReference() : null);
                    return;
                }
            }
        }

    }

    private static <T> ComponentServiceObjects<T> toComponentServiceObjects(ServiceObjects<T> serviceObjects) {
        return new ComponentServiceObjects<T>() {
            @Override
            public T getService() {
                return serviceObjects.getService();
            }
            @Override
            public void ungetService(T service) {
                serviceObjects.ungetService(service);
            }
            @Override
            public ServiceReference<T> getServiceReference() {
                return serviceObjects.getServiceReference();
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static void addToCollection(Object target, Field field, Object item) {
        try {
            field.setAccessible(true);
            Collection<Object> collection = (Collection<Object>)field.get(target);
            if (collection == null) {
                collection = newCollectionInstance(field.getType());
            }
            if (item != null) {
                collection.add(item);
            }
            field.set(target, collection);

        } catch (IllegalAccessException | IllegalArgumentException | InstantiationException ex) {
            throw new RuntimeException("Unable to set field '" + field.getName() + "' for class "
                    + target.getClass().getName(), ex);
        }
    }

    @SuppressWarnings("unchecked")
    private static void removeFromCollection(Object target, Field field, Object item) {
        try {
            field.setAccessible(true);
            Collection<Object> collection = (Collection<Object>)field.get(target);
            if (collection == null) {
                collection = newCollectionInstance(field.getType());
            }
            if (item != null) {
                collection.remove(item);
            }
            field.set(target, collection);

        } catch (IllegalAccessException | IllegalArgumentException | InstantiationException ex) {
            throw new RuntimeException("Unable to set field '" + field.getName() + "' for class "
                    + target.getClass().getName(), ex);
        }
    }

    @SuppressWarnings({ "unchecked", "null" })
    private static @NotNull Collection<Object> newCollectionInstance(Class<?> collectionType)
            throws InstantiationException, IllegalAccessException {
        if (collectionType == List.class || collectionType == Collection.class) {
            return new ArrayList<>();
        }
        if (collectionType == Set.class) {
            return new HashSet<>();
        }
        if (collectionType == SortedSet.class) {
            return new TreeSet<>();
        }
        return (Collection)collectionType.newInstance();
    }

    /**
     * Directly invoke bind method on service for the given reference.
     * @param reference Reference metadata
     * @param target Target object for reference
     * @param serviceInfo Service on which to invoke the method
     * @param bundleContext Bundle context
     */
    public static void invokeBindMethod(Reference reference, Object target, ServiceInfo serviceInfo, BundleContext bundleContext) {
        invokeBindUnbindMethod(reference,  target, serviceInfo, bundleContext, true);
    }

    /**
     * Directly invoke unbind method on service for the given reference.
     * @param reference Reference metadata
     * @param target Target object for reference
     * @param serviceInfo Service on which to invoke the method
     * @param bundleContext Bundle context
     */
    public static void invokeUnbindMethod(Reference reference, Object target, ServiceInfo serviceInfo, BundleContext bundleContext) {
        invokeBindUnbindMethod(reference,  target, serviceInfo, bundleContext, false);
    }

    @SuppressWarnings("unchecked")
    private static List<ServiceInfo<?>> getMatchingServices(Class<?> type, BundleContext bundleContext, String filter) {
        List<ServiceInfo<?>> matchingServices = new ArrayList<>();
        try {
            ServiceReference[] references = bundleContext.getServiceReferences(type.getName(), filter);
            if (references != null) {
                for (ServiceReference<?> serviceReference : references) {
                    Object serviceInstance = bundleContext.getService(serviceReference);
                    Map<String, Object> serviceConfig = new HashMap<>();
                    String[] keys = serviceReference.getPropertyKeys();
                    for (String key : keys) {
                        serviceConfig.put(key, serviceReference.getProperty(key));
                    }
                    matchingServices.add(new ServiceInfo(serviceInstance, serviceConfig, serviceReference));
                }
            }
        } catch (InvalidSyntaxException ex) {
            // ignore
        }
        return matchingServices;
    }

    /**
     * Collects all references of any registered service that match with any of the exported interfaces of the given service registration
     * and are defined as DYNAMIC.
     * @param registeredServices Registered Services
     * @param registration Service registration
     * @return List of references
     */
    @SuppressWarnings("unchecked")
    public static List<ReferenceInfo<?>> getMatchingDynamicReferences(SortedSet<MockServiceRegistration> registeredServices,
            MockServiceRegistration<?> registration) {
        List<ReferenceInfo<?>> references = new ArrayList<>();
        for (MockServiceRegistration<?> existingRegistration : registeredServices) {
            @SuppressWarnings("null")
            OsgiMetadata metadata = OsgiMetadataUtil.getMetadata(existingRegistration.getService().getClass());
            if (metadata != null) {
                for (Reference reference : metadata.getReferences()) {
                    if (reference.isConstructorParameter()) {
                        continue;
                    }
                    if (reference.getPolicy() == ReferencePolicy.DYNAMIC) {
                        for (String serviceInterface : registration.getClasses()) {
                            if (StringUtils.equals(serviceInterface, reference.getInterfaceType())) {
                                references.add(new ReferenceInfo(existingRegistration, reference));
                            }
                        }
                    }
                }
            }
        }
        return references;
    }

    /**
     * Collects all references of any registered service that match with any of the exported interfaces of the given service registration
     * and are defined as STATIC + GREEDY.
     * @param registeredServices Registered Services
     * @param registration Service registration
     * @return List of references
     */
    @SuppressWarnings({ "unchecked", "null" })
    public static List<ReferenceInfo<?>> getMatchingStaticGreedyReferences(SortedSet<MockServiceRegistration> registeredServices,
            MockServiceRegistration<?> registration) {
        List<ReferenceInfo<?>> references = new ArrayList<>();
        for (MockServiceRegistration<?> existingRegistration : registeredServices) {
            OsgiMetadata metadata = OsgiMetadataUtil.getMetadata(existingRegistration.getService().getClass());
            if (metadata != null) {
                for (Reference reference : metadata.getReferences()) {
                    if (reference.getPolicy() == ReferencePolicy.STATIC && reference.getPolicyOption() == ReferencePolicyOption.GREEDY) {
                        for (String serviceInterface : registration.getClasses()) {
                            if (StringUtils.equals(serviceInterface, reference.getInterfaceType())) {
                                references.add(new ReferenceInfo(existingRegistration, reference));
                            }
                        }
                    }
                }
            }
        }
        return references;
    }

    static class ServiceInfo<T> {

        private final T serviceInstance;
        private final Map<String, Object> serviceConfig;
        private final ServiceReference<T> serviceReference;

        public ServiceInfo(T serviceInstance, Map<String, Object> serviceConfig, ServiceReference<T> serviceReference) {
            this.serviceInstance = serviceInstance;
            this.serviceConfig = serviceConfig;
            this.serviceReference = serviceReference;
        }

        public ServiceInfo(MockServiceRegistration<T> registration) {
            this.serviceInstance = registration.getService();
            this.serviceConfig = registration.getPropertiesAsMap();
            this.serviceReference = registration.getReference();
        }

        public T getServiceInstance() {
            return this.serviceInstance;
        }

        public Map<String, Object> getServiceConfig() {
            return this.serviceConfig;
        }

        public ServiceReference<T> getServiceReference() {
            return serviceReference;
        }

    }

    static class ReferenceInfo<T> {

        private final MockServiceRegistration<T> serviceRegistration;
        private final Reference reference;

        public ReferenceInfo(MockServiceRegistration<T> serviceRegistration, Reference reference) {
            this.serviceRegistration = serviceRegistration;
            this.reference = reference;
        }

        public MockServiceRegistration<T> getServiceRegistration() {
            return serviceRegistration;
        }

        public Reference getReference() {
            return reference;
        }

    }

}
