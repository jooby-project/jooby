/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

public class UsageTest {

  static {
    System.setProperty("jooby.host", "http://localhost:4000");
  }

  @Test
  public void parameterNameMissing() throws NoSuchMethodException {
    Method method = getClass().getDeclaredMethod("parameterMissing", String.class);
    Usage usage = Usage.parameterNameNotPresent(method.getParameters()[0]);
    assertEquals(
        "Unable to provision parameter at position: '0', require by: method"
            + " io.jooby.UsageTest.parameterMissing(java.lang.String). Parameter's name is"
            + " missing\n"
            + "For more details, please visit:"
            + " http://localhost:4000/usage#bean-converter-parameter-name-missing",
        usage.getMessage());
  }

  public void parameterMissing(String some) {}
}
