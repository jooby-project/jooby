package io.jooby.internal;

import io.jooby.Route;
import io.jooby.internal.asm.ClassSource;
import io.jooby.internal.asm.Lambdas;
import io.jooby.internal.asm.MethodFinder;
import io.jooby.internal.asm.ReturnType;
import io.jooby.internal.asm.TypeParser;
import org.jooby.funzy.Throwing;
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

  public RouteAnalyzer(ClassLoader loader) {
    this(loader, false);
  }

  public java.lang.reflect.Type returnType(Route.Handler handler) {
    try {
      Method method = Lambdas.getLambdaMethod(handler);
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
}
