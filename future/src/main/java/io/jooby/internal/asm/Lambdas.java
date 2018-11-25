package io.jooby.internal.asm;

import org.jooby.funzy.Throwing;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;

public class Lambdas {

  // getting the SerializedLambda
  private static SerializedLambda getSerializedLambda(Object function) throws NoSuchMethodException {
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
      } catch (Throwable t) {
        throw Throwing.sneakyThrow(t);
      }
    }

    throw new NoSuchMethodException("writeReplace");
  }

  // getting the synthetic static lambda method
  public static Method getLambdaMethod(Serializable function) throws Exception {
    SerializedLambda lambda = getSerializedLambda(function);
    String implClassName = lambda.getImplClass().replace('/', '.');
    Class<?> implClass = Class.forName(implClassName);

    String lambdaName = lambda.getImplMethodName();

    for (Method m : implClass.getDeclaredMethods()) {
      if (m.getName().equals(lambdaName)) {
        return m;
      }
    }

    throw new NoSuchMethodException(lambdaName);
  }
}
