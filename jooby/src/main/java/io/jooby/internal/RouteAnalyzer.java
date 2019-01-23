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
package io.jooby.internal;

import io.jooby.Context;
import io.jooby.Route;
import io.jooby.internal.asm.ClassSource;
import io.jooby.internal.asm.Lambdas;
import io.jooby.internal.asm.MethodFinder;
import io.jooby.internal.asm.ReturnType;
import io.jooby.internal.asm.TypeParser;
import io.jooby.Throwing;
import org.objectweb.asm.ClassReader;

import java.io.PrintWriter;
import java.lang.reflect.Method;

public class RouteAnalyzer {

  private final TypeParser typeParser;
  private ClassSource source;
  private boolean debug;

  public RouteAnalyzer(ClassLoader loader, boolean debug) {
    this.source = new ClassSource(loader);
    this.typeParser = new TypeParser(loader);
    this.debug = debug;
  }

  public java.lang.reflect.Type returnType(Route.Handler handler) {
    try {
      Method method = Lambdas.getLambdaMethod(handler);
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
      throw Throwing.sneakyThrow(x);
    }
  }

  public ClassLoader getClassLoader() {
    return typeParser.getClassLoader();
  }
}
