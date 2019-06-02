/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.asm;

import io.jooby.Sneaky;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;

public class Lambdas {

  // getting the SerializedLambda
  private static SerializedLambda getSerializedLambda(Object function)
      throws NoSuchMethodException {
    for (Class<?> clazz = function.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
      try {
        Method replaceMethod = clazz.getDeclaredMethod("writeReplace");
        replaceMethod.setAccessible(true);
        Object serializedForm = replaceMethod.invoke(function);

        if (serializedForm instanceof SerializedLambda) {
          return (SerializedLambda) serializedForm;
        }
      } catch (NoSuchMethodException e) {
        // fall through the loop and try the next class
      } catch (Exception t) {
        throw Sneaky.propagate(t);
      }
    }

    return null;
  }

  // getting the synthetic static lambda method
  public static Method getLambdaMethod(ClassLoader loader, Object function) throws Exception {
    SerializedLambda lambda = getSerializedLambda(function);
    if (lambda != null) {
      String implClassName = lambda.getImplClass().replace('/', '.');
      Class<?> implClass = loader.loadClass(implClassName);//Class.forName(implClassName);

      String lambdaName = lambda.getImplMethodName();

      for (Method m : implClass.getDeclaredMethods()) {
        if (m.getName().equals(lambdaName)) {
          return m;
        }
      }
    }
    return null;
  }
}
