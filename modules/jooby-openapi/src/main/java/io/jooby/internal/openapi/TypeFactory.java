/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi;

import io.jooby.Context;
import io.jooby.Jooby;
import io.jooby.Route;
import io.jooby.Router;
import io.jooby.ValueNode;
import org.objectweb.asm.Type;

import java.util.Optional;

public class TypeFactory {
  public static final Type OBJECT = Type.getType(Object.class);

  public static final Type VOID = Type.getType(void.class);

  public static final Type VOID_OBJ = Type.getType(Void.class);

  public static final Type STRING = Type.getType(String.class);

  public static final Type STRING_ARRAY = Type.getType(String[].class);

  public static final Type HANDLER = Type.getType(Route.Handler.class);

  public static final Type ROUTER = Type.getType(Router.class);

  public static final Type JOOBY = Type.getType(Jooby.class);

  public static final Type CONTEXT = Type.getType(Context.class);

  public static final Type VALUE_NODE = Type.getType(ValueNode.class);

  public static final Type KOOBY = Type.getType("Lio/jooby/Kooby;");

  public static final Type KOOBYKT = Type.getType("Lio/jooby/KoobyKt;");

  public static final Type HANDLER_CONTEXT = Type.getType("Lio/jooby/HandlerContext;");

  public static final Type COROUTINE_ROUTER = Type.getType("Lio/jooby/CoroutineRouter;");

  public static final Type KT_KLASS = Type.getType("Lkotlin/reflect/KClass;");

  public static final Type KT_FUN_0 = Type.getType("Lkotlin/jvm/functions/Function0;");

  public static final Type KT_FUN_1 = Type.getType("Lkotlin/jvm/functions/Function1;");

  public static final Type KT_FUN_2 = Type.getType("Lkotlin/jvm/functions/Function2;");

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
    if (name.equals(Context.class.getName())) {
      return Optional.of(TypeFactory.CONTEXT);
    }
    return Optional.empty();
  }

  public static Type fromInternalName(String internalName) {
    return fromJavaClassName(internalName.replace("/", "."))
        .orElseGet(() -> Type.getObjectType(internalName));
  }
}
