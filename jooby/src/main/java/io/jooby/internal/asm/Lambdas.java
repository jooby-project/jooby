/**
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *    Copyright 2014 Edgar Espina
 */
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
