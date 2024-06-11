/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt;

import static io.jooby.internal.apt.AnnotationSupport.findAnnotationValue;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.VariableElement;

import io.jooby.apt.MvcContext;

public class MvcParameter {
  private static final Predicate<String> NULLABLE =
      name -> name.toLowerCase().endsWith(".nullable");
  private static final Predicate<String> NON_NULL =
      name -> name.toLowerCase().endsWith(".nonnull") || name.toLowerCase().endsWith(".notnull");
  private final VariableElement parameter;
  private final Map<String, AnnotationMirror> annotations;
  private final TypeDefinition type;

  public MvcParameter(MvcContext context, VariableElement parameter) {
    this.parameter = parameter;
    this.annotations = annotationMap(parameter);
    this.type =
        new TypeDefinition(context.getProcessingEnvironment().getTypeUtils(), parameter.asType());
  }

  public TypeDefinition getType() {
    return type;
  }

  public String generateMapping(boolean kt) {
    var strategy =
        annotations.entrySet().stream()
            .flatMap(
                it -> {
                  var found = ParameterGenerator.findByAnnotation(it.getKey());
                  return found == null
                      ? Stream.empty()
                      : Stream.of(Map.entry(found, it.getValue()));
                })
            .findFirst();
    var nameProvider =
        strategy
            .filter(
                it ->
                    !EnumSet.of(ParameterGenerator.BodyParam, ParameterGenerator.Lookup)
                        .contains(it.getKey()))
            .map(Map.Entry::getValue)
            .orElse(null);
    var defaultParameterName = parameter.getSimpleName().toString();
    var parameterName =
        nameProvider == null
            ? defaultParameterName
            : findAnnotationValue(nameProvider, AnnotationSupport.VALUE).stream()
                .findFirst()
                .orElse(defaultParameterName);
    var rawType = type.getRawType();
    var elementType =
        type.getArguments().isEmpty() ? rawType : type.getArguments().get(0).getRawType();
    // keep kotlin.coroutines.Continuation as main type
    var parameterType = elementType.toString();
    return switch (parameterType) {
        /* Type Injection: */
      case "io.jooby.Context" -> StringCodeBlock.of("ctx");
      case "io.jooby.QueryString" -> {
        if (type.is(Optional.class)) {
          yield StringCodeBlock.of("java.util.Optional.ofNullable(ctx.query())");
        } else {
          yield StringCodeBlock.of("ctx.query()");
        }
      }
      case "io.jooby.Formdata" -> StringCodeBlock.of("ctx.form()");
      case "io.jooby.FlashMap" -> StringCodeBlock.of("ctx.flash()");
      case "io.jooby.Body" -> StringCodeBlock.of("ctx.body()");
      case "io.jooby.Session" -> {
        if (type.is(Optional.class)) {
          yield StringCodeBlock.of("java.util.Optional.ofNullable(ctx.sessionOrNull())");
        } else {
          yield hasAnnotation(NULLABLE)
              ? StringCodeBlock.of("ctx.sessionOrNull()")
              : StringCodeBlock.of("ctx.session()");
        }
      }
      case "io.jooby.Route" -> StringCodeBlock.of("ctx.getRoute()");
        // FileUpload
      case "io.jooby.FileUpload" ->
          switch (rawType.toString()) {
            case "java.util.List" ->
                StringCodeBlock.of("ctx.files(", StringCodeBlock.string(parameterName), ")");
            case "java.util.Optional" ->
                StringCodeBlock.of(
                    "ctx.files(", StringCodeBlock.string(parameterName), ").stream().findFirst()");
            default -> StringCodeBlock.of("ctx.file(", StringCodeBlock.string(parameterName), ")");
          };
      case "java.nio.file.Path" ->
          StringCodeBlock.of("ctx.file(", StringCodeBlock.string(parameterName), ").path()");
      default -> {
        // By annotation type;
        if (strategy.isEmpty()) {
          // must be body
          yield ParameterGenerator.BodyParam.toSourceCode(
              kt, null, type, parameterName, isNullable(kt));
        } else {
          yield strategy
              .get()
              .getKey()
              .toSourceCode(kt, strategy.get().getValue(), type, parameterName, isNullable(kt));
        }
      }
    };
  }

  public boolean isNullable(boolean kt) {
    // Any that ends with `Nullable`
    if (hasAnnotation(NULLABLE)) {
      return true;
    }
    if (hasAnnotation(NON_NULL)) {
      return false;
    }
    if (kt) {
      // TODO: review Optional arguments
      return false;
    }
    return !parameter.asType().getKind().isPrimitive();
  }

  private boolean hasAnnotation(Predicate<String> predicate) {
    return annotations.keySet().stream().anyMatch(predicate);
  }

  private Map<String, AnnotationMirror> annotationMap(VariableElement parameter) {
    return Stream.of(parameter.getAnnotationMirrors(), parameter.asType().getAnnotationMirrors())
        .filter(Objects::nonNull)
        .flatMap(List::stream)
        .collect(Collectors.toMap(it -> it.getAnnotationType().toString(), Function.identity()));
  }
}
