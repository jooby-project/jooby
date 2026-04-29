/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

public class RouteMvcMethodTest {

  public static class Controller {
    public String hello(String name) {
      return "Hello " + name;
    }

    public String noArgs() {
      return "No args";
    }
  }

  @Test
  public void testToMethod() throws NoSuchMethodException {
    Route.MvcMethod mvc =
        new Route.MvcMethod(Controller.class, "hello", String.class, String.class);

    Method method = mvc.toMethod();

    assertEquals("hello", method.getName());
    assertEquals(Controller.class, method.getDeclaringClass());
    assertEquals(String.class, method.getReturnType());
    assertArrayEquals(new Class[] {String.class}, method.getParameterTypes());
  }

  @Test
  public void testToMethodNotFound() {
    Route.MvcMethod mvc = new Route.MvcMethod(Controller.class, "nonExistent", String.class);

    // Triggers SneakyThrows.propagate(NoSuchMethodException)
    assertThrows(NoSuchMethodException.class, mvc::toMethod);
  }

  @Test
  public void testToMethodHandle() throws Throwable {
    Route.MvcMethod mvc =
        new Route.MvcMethod(Controller.class, "hello", String.class, String.class);

    MethodHandle handle = mvc.toMethodHandle();

    assertNotNull(handle);
    Controller controller = new Controller();
    String result = (String) handle.invoke(controller, "Jooby");
    assertEquals("Hello Jooby", result);
  }

  @Test
  public void testToMethodHandleWithLookup() throws Throwable {
    Route.MvcMethod mvc =
        new Route.MvcMethod(Controller.class, "hello", String.class, String.class);
    MethodHandles.Lookup lookup = MethodHandles.publicLookup();

    MethodHandle handle = mvc.toMethodHandle(lookup);

    assertNotNull(handle);
    Controller controller = new Controller();
    String result = (String) handle.invoke(controller, "Jooby");
    assertEquals("Hello Jooby", result);
  }

  @Test
  public void testToMethodHandleIllegalAccess() {
    // Attempting to access a private method via a lookup that shouldn't have access
    class PrivateController {
      private void secret() {}
    }

    Route.MvcMethod mvc = new Route.MvcMethod(PrivateController.class, "secret", void.class);

    // Using publicLookup to force an IllegalAccessException during unreflect
    assertThrows(
        IllegalAccessException.class, () -> mvc.toMethodHandle(MethodHandles.publicLookup()));
  }

  @Test
  public void testRecordProperties() {
    Route.MvcMethod mvc =
        new Route.MvcMethod(Controller.class, "hello", String.class, String.class);

    assertEquals(Controller.class, mvc.declaringClass());
    assertEquals("hello", mvc.name());
    assertEquals(String.class, mvc.returnType());
    assertArrayEquals(new Class[] {String.class}, mvc.parameterTypes());
  }

  // --- Complementary tests for 100% JaCoCo Record Coverage ---

  @Test
  public void testNoArgsMethod() throws Exception {
    Route.MvcMethod mvc = new Route.MvcMethod(Controller.class, "noArgs", String.class);

    // Verifies the varargs edge case where the array length is 0
    assertEquals(0, mvc.parameterTypes().length);
    assertNotNull(mvc.toMethod());
  }

  @Test
  public void testEqualsAndHashCode() {
    Route.MvcMethod mvc1 =
        new Route.MvcMethod(Controller.class, "hello", String.class, String.class);
    Route.MvcMethod mvc2 =
        new Route.MvcMethod(Controller.class, "hello", String.class, String.class);
    Route.MvcMethod mvc3 = new Route.MvcMethod(Controller.class, "noArgs", String.class);

    // Verify auto-generated record equality and hashing logic
    assertEquals(mvc1, mvc2);
    assertEquals(mvc1.hashCode(), mvc2.hashCode());
    assertNotEquals(mvc1, mvc3);
    assertNotEquals(mvc1.hashCode(), mvc3.hashCode());
    assertNotEquals(mvc1, null);
  }

  @Test
  public void testToString() {
    Route.MvcMethod mvc =
        new Route.MvcMethod(Controller.class, "hello", String.class, String.class);

    // Verify auto-generated record stringification logic
    String str = mvc.toString();
    assertTrue(str.contains("MvcMethod"));
    assertTrue(str.contains("hello"));
    assertTrue(str.contains("Controller"));
    assertTrue(str.contains("java.lang.String"), "Should contain array content representation");
  }
}
