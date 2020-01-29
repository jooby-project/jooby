package io.jooby.internal.openapi;

import io.jooby.Jooby;
import io.jooby.Route;
import io.jooby.Router;
import org.objectweb.asm.Type;

import java.util.Optional;

public class TypeFactory {

  public static final Type STRING = Type.getType(String.class);

  public static final Type HANDLER = Type.getType(Route.Handler.class);

  public static final Type ROUTER = Type.getType(Router.class);

  public static final Type JOOBY = Type.getType(Jooby.class);

  public static final Type KOOBY = Type.getType("Lio/jooby/Kooby;");

  public static final Type KT_FUN_1 = Type.getType("Lkotlin/jvm/functions/Function1;");

  public static Type fromJavaName(String name) {
    return fromJavaClassName(name).orElseGet(() -> Type.getObjectType(name.replace(".", "/")));
  }

  public static Type from(Class klass) {
    return fromJavaClassName(klass.getName()).orElseGet(() -> Type.getType(klass));
  }

  private static Optional<Type> fromJavaClassName(String name) {
    if (name.equals(String.class.getName())) {
      return Optional.of(TypeFactory.STRING);
    }
    if (name.equals(Route.Handler.class.getName())) {
      return Optional.of(TypeFactory.HANDLER);
    }
    return Optional.empty();
  }

  public static Type fromInternalName(String internalName) {
    return fromJavaClassName(internalName.replace("/", "."))
        .orElseGet(() -> Type.getObjectType(internalName));
  }
}
