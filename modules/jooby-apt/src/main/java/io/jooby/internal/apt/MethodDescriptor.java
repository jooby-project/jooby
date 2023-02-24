/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt;

import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.objectweb.asm.Type;

public class MethodDescriptor {
  private Type declaringType;

  private String descriptor;

  private String name;

  public MethodDescriptor(Type declaringType, String name, String... argsReturnType) {
    this.declaringType = declaringType;
    this.name = name;
    this.descriptor = buildDescriptor(argsReturnType);
  }

  private String buildDescriptor(String[] argsReturnType) {
    if (argsReturnType.length == 0) {
      return "()";
    }
    return "("
        + Stream.of(argsReturnType)
            .limit(argsReturnType.length - 1)
            .map(this::classNameToTypeDescriptor)
            .collect(Collectors.joining())
        + ")"
        + classNameToTypeDescriptor(argsReturnType[argsReturnType.length - 1]);
  }

  private String classNameToTypeDescriptor(String classname) {
    switch (classname) {
      case "boolean":
        return "Z";
      case "[B":
        return classname;
      case "byte":
        return "B";
      case "int":
        return "I";
      case "long":
        return "J";
      case "float":
        return "F";
      case "double":
        return "D";
      default:
        {
          String descriptor = classname.replace(".", "/");
          if (descriptor.startsWith("[L")) {
            // array
            return descriptor;
          }
          return "L" + descriptor + ";";
        }
    }
  }

  public static class Reified {
    public static MethodDescriptor map() {
      return new MethodDescriptor(
          JoobyTypes.Reified,
          "map",
          java.lang.reflect.Type.class.getName(),
          java.lang.reflect.Type.class.getName(),
          "io.jooby.Reified");
    }

    public static MethodDescriptor getParameterized() {
      return new MethodDescriptor(
          JoobyTypes.Reified,
          "getParameterized",
          java.lang.reflect.Type.class.getName(),
          java.lang.reflect.Type[].class.getName(),
          "io.jooby.Reified");
    }

    public static MethodDescriptor getType() {
      return new MethodDescriptor(
          JoobyTypes.Reified, "getType", java.lang.reflect.Type.class.getName());
    }
  }

  public static class Route {
    public static MethodDescriptor attribute() {
      return new MethodDescriptor(
          JoobyTypes.Route,
          "attribute",
          String.class.getName(),
          Object.class.getName(),
          "io.jooby.Route");
    }

    public static MethodDescriptor setReturnType() {
      return new MethodDescriptor(
          JoobyTypes.Route,
          "setReturnType",
          java.lang.reflect.Type.class.getName(),
          "io.jooby.Route");
    }

    public static MethodDescriptor setExecutorKey() {
      return new MethodDescriptor(
          JoobyTypes.Route, "setExecutorKey", String.class.getName(), "io.jooby.Route");
    }
  }

  public static class Value {
    public static MethodDescriptor value() {
      return new MethodDescriptor(JoobyTypes.Value, "value", String.class.getName());
    }

    public static MethodDescriptor valueOrNull() {
      return new MethodDescriptor(JoobyTypes.Value, "valueOrNull", String.class.getName());
    }

    public static MethodDescriptor intValue() {
      return new MethodDescriptor(JoobyTypes.Value, "intValue", int.class.getName());
    }

    public static MethodDescriptor byteValue() {
      return new MethodDescriptor(JoobyTypes.Value, "byteValue", byte.class.getName());
    }

    public static MethodDescriptor longValue() {
      return new MethodDescriptor(JoobyTypes.Value, "longValue", long.class.getName());
    }

    public static MethodDescriptor floatValue() {
      return new MethodDescriptor(JoobyTypes.Value, "floatValue", float.class.getName());
    }

    public static MethodDescriptor doubleValue() {
      return new MethodDescriptor(JoobyTypes.Value, "doubleValue", double.class.getName());
    }

    public static MethodDescriptor booleanValue() {
      return new MethodDescriptor(JoobyTypes.Value, "booleanValue", boolean.class.getName());
    }

    public static MethodDescriptor toOptional() {
      return new MethodDescriptor(JoobyTypes.Value, "toOptional", Optional.class.getName());
    }

    public static MethodDescriptor toList() {
      return new MethodDescriptor(JoobyTypes.Value, "toList", List.class.getName());
    }

    public static MethodDescriptor toSet() {
      return new MethodDescriptor(JoobyTypes.Value, "toSet", Set.class.getName());
    }
  }

  public static class ValueNode {
    public static MethodDescriptor toOptional() {
      return new MethodDescriptor(
          JoobyTypes.ValueNode, "toOptional", Class.class.getName(), Optional.class.getName());
    }

    public static MethodDescriptor toList() {
      return new MethodDescriptor(
          JoobyTypes.ValueNode, "toList", Class.class.getName(), List.class.getName());
    }

    public static MethodDescriptor toSet() {
      return new MethodDescriptor(
          JoobyTypes.ValueNode, "toSet", Class.class.getName(), Set.class.getName());
    }

    public static MethodDescriptor to() {
      return new MethodDescriptor(
          JoobyTypes.ValueNode, "to", Class.class.getName(), Object.class.getName());
    }
  }

  public static class FileUpload {
    public static MethodDescriptor path() {
      return new MethodDescriptor(JoobyTypes.FileUpload, "path", Path.class.getName());
    }
  }

  public static class Body {
    public static MethodDescriptor bytes() {
      return new MethodDescriptor(JoobyTypes.Body, "bytes", byte[].class.getName());
    }

    public static MethodDescriptor channel() {
      return new MethodDescriptor(JoobyTypes.Body, "channel", ReadableByteChannel.class.getName());
    }

    public static MethodDescriptor stream() {
      return new MethodDescriptor(JoobyTypes.Body, "stream", InputStream.class.getName());
    }
  }

  public static class Context {
    public static MethodDescriptor file() {
      return new MethodDescriptor(
          JoobyTypes.Context, "file", String.class.getName(), "io.jooby.FileUpload");
    }

    public static MethodDescriptor bodyClass() {
      return new MethodDescriptor(
          JoobyTypes.Context, "body", Class.class.getName(), Object.class.getName());
    }

    public static MethodDescriptor bodyType() {
      return new MethodDescriptor(
          JoobyTypes.Context,
          "body",
          java.lang.reflect.Type.class.getName(),
          Object.class.getName());
    }

    public static MethodDescriptor body() {
      return new MethodDescriptor(JoobyTypes.Context, "body", "io.jooby.Body");
    }

    public static MethodDescriptor getRoute() {
      return new MethodDescriptor(JoobyTypes.Context, "getRoute", "io.jooby.Route");
    }

    public static MethodDescriptor lookup() {
      return new MethodDescriptor(
          JoobyTypes.Context,
          "lookup",
          String.class.getName(),
          "[Lio.jooby.ParamSource;",
          JoobyTypes.Value.getClassName());
    }

    public static MethodDescriptor form() {
      // String
      return new MethodDescriptor(
          JoobyTypes.Context, "form", String.class.getName(), JoobyTypes.ValueNode.getClassName());
    }

    public static MethodDescriptor formBody() {
      return new MethodDescriptor(JoobyTypes.Context, "form", JoobyTypes.Formdata.getClassName());
    }

    public static MethodDescriptor flashMap() {
      return new MethodDescriptor(JoobyTypes.Context, "flash", JoobyTypes.FlashMap.getClassName());
    }

    public static MethodDescriptor flash() {
      return new MethodDescriptor(
          JoobyTypes.Context, "flash", String.class.getName(), JoobyTypes.Value.getClassName());
    }

    public static MethodDescriptor header() {
      return new MethodDescriptor(
          JoobyTypes.Context, "header", String.class.getName(), JoobyTypes.Value.getClassName());
    }

    public static MethodDescriptor cookie() {
      return new MethodDescriptor(
          JoobyTypes.Context, "cookie", String.class.getName(), JoobyTypes.Value.getClassName());
    }

    public static MethodDescriptor query() {
      // query(String)
      return new MethodDescriptor(
          JoobyTypes.Context, "query", String.class.getName(), JoobyTypes.ValueNode.getClassName());
    }

    public static MethodDescriptor queryString() {
      return new MethodDescriptor(JoobyTypes.Context, "query", "io.jooby.QueryString");
    }

    public static MethodDescriptor session() {
      return new MethodDescriptor(
          JoobyTypes.Context, "session", String.class.getName(), JoobyTypes.Value.getClassName());
    }

    public static MethodDescriptor sessionMap() {
      // session()
      return new MethodDescriptor(JoobyTypes.Context, "session", "io.jooby.Session");
    }

    public static MethodDescriptor sessionOrNull() {
      return new MethodDescriptor(JoobyTypes.Context, "sessionOrNull", "io.jooby.Session");
    }

    public static MethodDescriptor pathMap() {
      // path();
      return new MethodDescriptor(JoobyTypes.Context, "path", JoobyTypes.ValueNode.getClassName());
    }

    public static MethodDescriptor path() {
      // path(String);
      return new MethodDescriptor(
          JoobyTypes.Context, "path", String.class.getName(), JoobyTypes.Value.getClassName());
    }

    public static MethodDescriptor getAttributes() {
      return new MethodDescriptor(JoobyTypes.Context, "getAttributes", Map.class.getName());
    }

    public static MethodDescriptor getAttribute() {
      return new MethodDescriptor(
          JoobyTypes.Context, "getAttribute", String.class.getName(), Object.class.getName());
    }

    public static MethodDescriptor filesWithName() {
      return new MethodDescriptor(
          JoobyTypes.Context, "files", String.class.getName(), List.class.getName());
    }

    public static MethodDescriptor files() {
      return new MethodDescriptor(JoobyTypes.Context, "files", List.class.getName());
    }

    public static MethodDescriptor setResponseCode() {
      return new MethodDescriptor(
          JoobyTypes.Context,
          "setResponseCode",
          "io.jooby.StatusCode",
          JoobyTypes.Context.getClassName());
    }

    public static MethodDescriptor getResponseCode() {
      return new MethodDescriptor(JoobyTypes.Context, "getResponseCode", "io.jooby.StatusCode");
    }
  }

  public String getName() {
    return name;
  }

  public String getDescriptor() {
    return descriptor;
  }

  public Type getDeclaringType() {
    return declaringType;
  }

  public Type getReturnType() {
    return Type.getReturnType(getDescriptor());
  }

  public Type[] getArgumentTypes() {
    return Type.getArgumentTypes(getDescriptor());
  }
}
