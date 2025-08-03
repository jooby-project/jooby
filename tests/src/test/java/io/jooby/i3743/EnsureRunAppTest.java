/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3743;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import io.jooby.Jooby;
import io.jooby.kt.KoobyKt;

public class EnsureRunAppTest {
  private static final Map<Type, Type> TO_JAVA =
      Map.of(
          kotlin.jvm.functions.Function0.class, Supplier.class,
          kotlin.jvm.functions.Function1.class, Consumer.class,
          kotlin.jvm.functions.Function0[].class, List.class);

  @Test
  public void ensureRunApp() {
    var javaRunApp = runAppMethods(Jooby.class);
    var kotlinRunApp = runAppMethods(KoobyKt.class);
    assertEquals(javaRunApp.size(), kotlinRunApp.size());
    for (var method : javaRunApp) {
      kotlinRunApp.stream()
          .filter(it -> matches(method.getParameterTypes(), it.getParameterTypes()))
          .findFirst()
          .orElseThrow(() -> new AssertionError("Method " + method + " not found on Kotlin"));
    }
  }

  private static List<Method> runAppMethods(Class<?> owner) {
    return Stream.of(owner.getDeclaredMethods())
        .filter(method -> method.getName().equals("runApp"))
        .filter(
            method ->
                Modifier.isStatic(method.getModifiers())
                    && Modifier.isPublic(method.getModifiers()))
        .toList();
  }

  private boolean matches(Class<?>[] javaParams, Class<?>[] ktParams) {
    if (javaParams.length != ktParams.length) {
      return false;
    }
    for (int i = 0; i < javaParams.length; i++) {
      if (!javaParams[i].equals(TO_JAVA.getOrDefault(ktParams[i], ktParams[i]))) {
        return false;
      }
    }
    return true;
  }
}
