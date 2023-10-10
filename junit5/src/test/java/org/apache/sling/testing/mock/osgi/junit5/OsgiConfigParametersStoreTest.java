package org.apache.sling.testing.mock.osgi.junit5;

import org.apache.sling.testing.mock.osgi.context.OsgiContextImpl;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OsgiConfigParametersStoreTest {

    private OsgiContextImpl context = new OsgiContextImpl();

    @Test
    void accessOsgiContextImplField() throws Exception {
        final Field contextField = getClass().getDeclaredField("context");
        assertThrows(IllegalStateException.class,
                () -> OsgiConfigParametersStore.accessOsgiContextImplField(contextField, this));

        final Field accessibleField = OsgiConfigParametersStore.getFieldFromTestClass(getClass()).orElseThrow();
        assertSame(context, OsgiConfigParametersStore.accessOsgiContextImplField(accessibleField, this));
    }
}