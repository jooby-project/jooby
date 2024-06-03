/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt;

import static java.util.stream.Collectors.joining;

import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.time.Period;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Stream;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.VariableElement;

import com.squareup.javapoet.CodeBlock;

public enum ParameterGenerator {
  ContextParam("getAttribute", "io.jooby.annotation.ContextParam", "jakarta.ws.rs.core.Context") {
    @Override
    public CodeBlock toSourceCode(
        AnnotationMirror annotation, TypeDefinition type, String name, boolean nullable) {
      if (type.is(Map.class)) {
        return CodeBlock.of(
            "java.util.Optional.ofNullable((java.util.Map) ctx.getAttribute($S)).orElseGet(() ->"
                + " ctx.getAttributes())",
            name);
      } else {
        return CodeBlock.of("($L) ctx.$L($S)", type.getRawType().toString(), method, name);
      }
    }
  },
  CookieParam("cookie", "io.jooby.annotation.CookieParam", "jakarta.ws.rs.CookieParam"),
  FlashParam("flash", "io.jooby.annotation.FlashParam"),
  FormParam("form", "io.jooby.annotation.FormParam", "jakarta.ws.rs.FormParam"),
  HeaderParam("header", "io.jooby.annotation.HeaderParam", "jakarta.ws.rs.HeaderParam"),
  Lookup("lookup", "io.jooby.annotation.Param"),
  PathParam("path", "io.jooby.annotation.PathParam", "jakarta.ws.rs.PathParam"),
  QueryParam("query", "io.jooby.annotation.QueryParam", "jakarta.ws.rs.QueryParam"),
  SessionParam("session", "io.jooby.annotation.SessionParam"),
  BodyParam("body") {
    @Override
    public CodeBlock toSourceCode(
        AnnotationMirror annotation, TypeDefinition type, String name, boolean nullable) {
      var rawType = type.getRawType().toString();
      return switch (rawType) {
        case "byte[]" -> CodeBlock.of("ctx.body().bytes()");
        case "java.io.InputStream" -> CodeBlock.of("ctx.body().stream()");
        case "java.nio.channels.ReadableByteChannel" -> CodeBlock.of("ctx.body().channel()");
        default -> {
          if (type.isPrimitive()) {
            yield CodeBlock.of("ctx.$L().$LValue()", method, type.getName());
          } else if (type.is(String.class)) {
            yield nullable
                ? CodeBlock.of("ctx.$L().valueOrNull()", method)
                : CodeBlock.of("ctx.$L().value()", method);
          } else if (type.is(Optional.class)) {
            yield CodeBlock.of(
                "ctx.$L().toOptional($L)", method, type.getArguments().get(0).toSourceCode());

          } else {
            yield CodeBlock.of("ctx.$L($L)", method, type.toSourceCode());
          }
        }
      };
    }
  };

  public CodeBlock toSourceCode(
      AnnotationMirror annotation, TypeDefinition type, String name, boolean nullable) {
    var paramSource = source(annotation);
    var builtin = builtinType(annotation, type, name, nullable);
    if (builtin == null) {
      var toValue =
          CONTAINER.stream()
              .filter(type::is)
              .findFirst()
              .map(Class::getSimpleName)
              .map(it -> "to" + it)
              .orElse(nullable ? "toNullable" : "to");
      var elementType = type.getArguments().isEmpty() ? type : type.getArguments().get(0);
      if (paramSource.isEmpty() && BUILT_IN.stream().noneMatch(elementType::is)) {
        // for unsupported types, we check if node with matching name is present, if not we fallback
        // to entire scope converter
        return CodeBlock.of(
            "ctx.$1L($2S).isMissing() ? ctx.$1L().$3L($4L.class) : ctx.$1L($2S).$3L($4L.class)",
            method,
            name,
            toValue,
            elementType.getName());
      } else {
        // container of supported types: List<Integer>, Optional<UUID>
        if (elementType.is(String.class)) {
          return CodeBlock.of("ctx.$L($S$L).$L()", method, name, paramSource, toValue);
        } else {
          return CodeBlock.of(
              "ctx.$L($S$L).$L($L.class)",
              method,
              name,
              paramSource,
              toValue,
              elementType.getName());
        }
      }
    } else {
      return builtin;
    }
  }

  protected CodeBlock builtinType(
      AnnotationMirror annotation, TypeDefinition type, String name, boolean nullable) {
    if (BUILT_IN.stream().anyMatch(type::is)) {
      var paramSource = source(annotation);
      // look at named parameter
      if (type.isPrimitive()) {
        // like: .intValue
        return CodeBlock.of("ctx.$L($S$L).$LValue()", method, name, paramSource, type.getName());
      } else if (type.is(String.class)) {
        var stringValue = nullable ? "valueOrNull" : "value";
        // StringL: .value
        return CodeBlock.of("ctx.$L($S$L).$L()", method, name, paramSource, stringValue);
      } else {
        var toValue = nullable ? "toNullable" : "to";
        // Any other type: .to(UUID.class)
        return CodeBlock.of(
            "ctx.$L($S$L).$L($L.class)", method, name, paramSource, toValue, type.getName());
      }
    }
    return null;
  }

  public static ParameterGenerator findByAnnotation(String annotation) {
    return Stream.of(values())
        .filter(it -> it.annotations.contains(annotation))
        .findFirst()
        .orElse(null);
  }

  ParameterGenerator(String method, String... annotations) {
    this.method = method;
    this.annotations = Set.of(annotations);
  }

  protected String source(AnnotationMirror annotation) {
    if (ParameterGenerator.Lookup.annotations.contains(annotation.getAnnotationType().toString())) {
      var sources =
          Annotations.attribute(
              annotation,
              "value",
              it -> {
                var value = it.getValue();
                if (value instanceof VariableElement variable) {
                  return variable.getSimpleName().toString();
                } else {
                  return value.toString();
                }
              });
      return sources.isEmpty()
          ? ""
          : sources.stream()
              .map(it -> "io.jooby.ParamSource." + it)
              .collect(joining(", ", ", ", ""));
    }
    return "";
  }

  protected final String method;
  private final Set<String> annotations;
  private static final Set<Class> CONTAINER = Set.of(List.class, Set.class, Optional.class);
  private static final Set<String> BUILT_IN =
      Set.of(
          String.class.getName(),
          Boolean.class.getName(),
          Boolean.TYPE.getName(),
          Byte.class.getName(),
          Byte.TYPE.getName(),
          Character.class.getName(),
          Character.TYPE.getName(),
          Short.class.getName(),
          Short.TYPE.getName(),
          Integer.class.getName(),
          Integer.TYPE.getName(),
          Long.class.getName(),
          Long.TYPE.getName(),
          Float.class.getName(),
          Float.TYPE.getName(),
          Double.class.getName(),
          Double.TYPE.getName(),
          Enum.class.getName(),
          java.util.UUID.class.getName(),
          java.time.Instant.class.getName(),
          java.util.Date.class.getName(),
          java.time.LocalDate.class.getName(),
          java.time.LocalDateTime.class.getName(),
          java.math.BigDecimal.class.getName(),
          java.math.BigInteger.class.getName(),
          Duration.class.getName(),
          Period.class.getName(),
          java.nio.charset.Charset.class.getName(),
          "io.jooby.StatusCode",
          TimeZone.class.getName(),
          ZoneId.class.getName(),
          URI.class.getName(),
          URL.class.getName());
}
