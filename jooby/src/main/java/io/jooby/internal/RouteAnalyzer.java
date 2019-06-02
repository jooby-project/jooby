/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.Context;
import io.jooby.Sneaky;
import io.jooby.internal.asm.ClassSource;
import io.jooby.internal.asm.Lambdas;
import io.jooby.internal.asm.MethodFinder;
import io.jooby.internal.asm.ReturnType;
import io.jooby.internal.asm.TypeParser;
import org.objectweb.asm.ClassReader;

import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public class RouteAnalyzer {

  private static final String CONTINUATION = "kotlin.coroutines.Continuation";
  private final TypeParser typeParser;
  private ClassSource source;
  private boolean debug;

  public RouteAnalyzer(ClassSource source, boolean debug) {
    this.source = source;
    this.typeParser = new TypeParser(source.getLoader());
    this.debug = debug;
  }

  public java.lang.reflect.Type returnType(Object handler) {
    try {
      Method method = methodHandler(handler);
      if (method == null) {
        return Object.class;
      }
      Class<?> returnType = method.getReturnType();
      if (returnType != Object.class) {
        return method.getGenericReturnType();
      }
      ClassReader reader = new ClassReader(source.byteCode(method.getDeclaringClass()));
      MethodFinder visitor = new MethodFinder(method, debug);
      reader.accept(visitor, 0);
      ReturnType returnTypeVisitor = new ReturnType(typeParser, visitor.node);

      if (debug) {
        System.out.println(method);
        PrintWriter writer = new PrintWriter(System.out);
        visitor.printer.print(writer);
        writer.flush();
      }

      return returnTypeVisitor.returnType();
    } catch (Exception x) {
      throw Sneaky.propagate(x);
    }
  }

  private Method methodHandler(Object handler) throws Exception {
    Method result = Lambdas.getLambdaMethod(this.source.getLoader(), handler);
    if (result == null) {
      // Kotlin?
      Method[] methods = handler.getClass().getDeclaredMethods();
      for (Method method : methods) {
        if (isKotlinApply(method) || isKotlinInvoke(method) || isKotlinContinuation(method)) {
          if (result == null) {
            result = method;
          } else {
            // choose more specific return type
            if (result.getReturnType() == Object.class) {
              result = method;
            }
          }
        }
      }
    }
    return result;
  }

  private boolean isKotlinApply(Method method) {
    return isContextFunction(method, "apply");
  }

  private boolean isKotlinInvoke(Method method) {
    return isContextFunction(method, "invoke");
  }

  private boolean isKotlinContinuation(Method method) {
    if (method.getName().equals("create") && method.getReturnType().getName()
        .equals(CONTINUATION)) {
      Parameter[] parameters = method.getParameters();
      if (parameters.length > 0) {
        return parameters[parameters.length - 1].getType().getName().equals(CONTINUATION);
      }
    }
    return false;
  }

  private boolean isContextFunction(Method method, String name) {
    if (method.getName().equals(name)) {
      Parameter[] parameters = method.getParameters();
      return parameters.length == 1 && parameters[0].getType() == Context.class;
    }
    return false;
  }
}
