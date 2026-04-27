/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Constructor;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RequestScopeTest {

  @AfterEach
  void tearDown() {
    // Ensure the ThreadLocal is cleared after each test to prevent state bleeding
    RequestScope.threadLocal().remove();
  }

  @Test
  @DisplayName("Test private constructor for full line coverage")
  void testPrivateConstructor() throws Exception {
    Constructor<RequestScope> constructor = RequestScope.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    RequestScope instance = constructor.newInstance();
    assertNotNull(instance);
  }

  @Test
  @DisplayName("Test empty state behavior (null context map)")
  void testEmptyState() {
    // Map is null at this point
    assertFalse(RequestScope.hasBind("key"));
    assertNull(RequestScope.get("key"));
    assertNull(RequestScope.unbind("key"));
  }

  @Test
  @DisplayName("Test bind, get, and hasBind logic")
  void testBindAndGet() {
    // 1. Map is null, createMap = true
    String previous = RequestScope.bind("myKey", "myValue");
    assertNull(previous);

    // 2. hasBind branches
    assertTrue(RequestScope.hasBind("myKey"));
    assertFalse(RequestScope.hasBind("missingKey"));

    // 3. get branches
    assertEquals("myValue", RequestScope.get("myKey"));
    assertNull(RequestScope.get("missingKey"));

    // 4. Bind on existing key (returns old value)
    String old = RequestScope.bind("myKey", "newValue");
    assertEquals("myValue", old);
    assertEquals("newValue", RequestScope.get("myKey"));
  }

  @Test
  @DisplayName("Test unbind and internal cleanup branches")
  void testUnbindAndCleanup() {
    // Unbind when contextMap is completely null
    assertNull(RequestScope.unbind("someKey"));

    // Setup: Bind two distinct keys
    RequestScope.bind("k1", "v1");
    RequestScope.bind("k2", "v2");

    ThreadLocal<Map<Object, Object>> tl = RequestScope.threadLocal();
    assertNotNull(tl.get());
    assertEquals(2, tl.get().size());

    // 1. Unbind one key -> map not empty -> ctx.isEmpty() == false
    String removed1 = RequestScope.unbind("k1");
    assertEquals("v1", removed1);
    assertNotNull(tl.get(), "ThreadLocal map should not be removed yet");
    assertEquals(1, tl.get().size());

    // 2. Unbind missing key -> map not empty
    assertNull(RequestScope.unbind("missingKey"));

    // 3. Unbind last key -> map becomes empty -> ctx.isEmpty() == true -> CONTEXT_TL.remove()
    String removed2 = RequestScope.unbind("k2");
    assertEquals("v2", removed2);
    assertNull(tl.get(), "ThreadLocal map should be completely removed now");

    // 4. Unbind again when contextMap has been removed
    assertNull(RequestScope.unbind("k2"));
  }

  @Test
  @DisplayName("Test threadLocal instance exposure")
  void testThreadLocal() {
    ThreadLocal<Map<Object, Object>> tl1 = RequestScope.threadLocal();
    ThreadLocal<Map<Object, Object>> tl2 = RequestScope.threadLocal();

    assertNotNull(tl1);
    assertSame(tl1, tl2, "Should return the exact same ThreadLocal instance");
  }
}
