/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.junit;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayNameGenerator;

public class CleanMethodNameGenerator extends DisplayNameGenerator.Standard {
  @Override
  public String generateDisplayNameForMethod(
      List<Class<?>> enclosingInstanceTypes, Class<?> testClass, Method testMethod) {
    // remove (ServerTestRunner) from test name:
    var args =
        Stream.of(testMethod.getParameters())
            .filter(param -> !param.getType().equals(ServerTestRunner.class))
            .toList();
    return args.isEmpty()
        ? testMethod.getName()
        : super.generateDisplayNameForMethod(enclosingInstanceTypes, testClass, testMethod);
  }
}
