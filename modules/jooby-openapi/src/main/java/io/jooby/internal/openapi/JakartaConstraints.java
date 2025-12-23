/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import com.google.common.base.CaseFormat;
import io.swagger.v3.oas.models.media.Schema;

public class JakartaConstraints {

  public static void apply(ClassNode node, Schema<?> schema) {
    if (schema.getProperties() != null) {
      schema
          .getProperties()
          .forEach(
              (property, value) -> {
                var annotations = getAnnotations(node, property);
                apply(value, annotations);
              });
    }
  }

  private static List<AnnotationNode> getAnnotations(ClassNode node, String property) {
    var methods =
        Optional.ofNullable(node.methods).orElse(List.of()).stream()
            .filter(
                method ->
                    method.name.equals(property)
                        || method.name.equals(
                            "get" + CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, property)))
            .filter(method -> method.parameters == null || !method.parameters.isEmpty())
            .filter(method -> (method.access & Opcodes.ACC_PUBLIC) != 0)
            .flatMap(
                method ->
                    method.visibleAnnotations == null
                        ? Stream.empty()
                        : method.visibleAnnotations.stream())
            .toList();
    var fields =
        Optional.ofNullable(node.fields).orElse(List.of()).stream()
            .filter(field -> field.name.equals(property))
            .filter(field -> (field.access & Opcodes.ACC_STATIC) == 0)
            .flatMap(
                method ->
                    method.visibleAnnotations == null
                        ? Stream.empty()
                        : method.visibleAnnotations.stream())
            .toList();
    return Stream.concat(methods.stream(), fields.stream()).toList();
  }

  public static void apply(Schema<?> schema, List<AnnotationNode> annotations) {
    //    AssertFalse
    //    AssertTrue
    for (var annotation : annotations) {
      switch (annotation.desc) {
        case "Ljakarta/validation/constraints/Digits;":
          {
            var integer =
                AnnotationUtils.findAnnotationValue(annotation, "integer")
                    .map(Object::toString)
                    .map(Integer::valueOf)
                    .orElse(null);
            var fraction =
                AnnotationUtils.findAnnotationValue(annotation, "fraction")
                    .map(Object::toString)
                    .map(Integer::valueOf)
                    .orElse(null);
            if (integer != null && fraction != null) {
              var multipleOf = BigDecimal.ONE.divide(BigDecimal.TEN.pow(fraction));
              var maximum = BigDecimal.TEN.pow(integer).subtract(multipleOf);
              schema.setMaximum(maximum);
              schema.setMultipleOf(multipleOf);
              schema.setType("number");
            }
          }
          break;
        case "Ljakarta/validation/constraints/Email;":
          {
            schema.setFormat("email");
          }
          break;
        case "Ljakarta/validation/constraints/DecimalMin;":
          {
            AnnotationUtils.findAnnotationValue(annotation, "value")
                .map(Object::toString)
                .ifPresent(
                    value -> {
                      schema.setMinimum(new BigDecimal(value));
                      var inclusive =
                          AnnotationUtils.findAnnotationValue(annotation, "inclusive")
                              .map(Object::toString)
                              .map(Boolean::valueOf)
                              .orElse(Boolean.TRUE);
                      if (!inclusive) {
                        schema.setExclusiveMinimum(true);
                      }
                    });
          }
          break;
        case "Ljakarta/validation/constraints/DecimalMax;":
          {
            AnnotationUtils.findAnnotationValue(annotation, "value")
                .map(Object::toString)
                .ifPresent(
                    value -> {
                      schema.setMaximum(new BigDecimal(value));
                      var inclusive =
                          AnnotationUtils.findAnnotationValue(annotation, "inclusive")
                              .map(Object::toString)
                              .map(Boolean::valueOf)
                              .orElse(Boolean.TRUE);
                      if (!inclusive) {
                        schema.setExclusiveMinimum(true);
                      }
                    });
          }
          break;
        // Ignored
        // case "Ljakarta/validation/constraints/Future;":{}break;
        // case "Ljakarta/validation/constraints/FutureOrPresent;":{}break;
        case "Ljakarta/validation/constraints/Max;":
          {
            AnnotationUtils.findAnnotationValue(annotation, "value")
                .map(Long.class::cast)
                .ifPresent(
                    value -> {
                      schema.setMaximum(new BigDecimal(value.toString()));
                    });
          }
          break;
        case "Ljakarta/validation/constraints/Min;":
          {
            AnnotationUtils.findAnnotationValue(annotation, "value")
                .map(Object::toString)
                .ifPresent(
                    value -> {
                      schema.setMinimum(new BigDecimal(value));
                    });
          }
          break;
        case "Ljakarta/validation/constraints/Negative;":
          {
            schema.setMaximum(BigDecimal.ZERO);
            schema.setExclusiveMaximum(true);
          }
          break;
        case "Ljakarta/validation/constraints/NegativeOrZero;":
          {
            schema.setMaximum(BigDecimal.ZERO);
          }
          break;
        case "Ljakarta/validation/constraints/NotBlank;":
          {
            schema.setPattern(".*\\S.*");
            schema.setMinLength(1);
          }
          break;
        case "Ljakarta/validation/constraints/NotEmpty;":
          {
            schema.minLength(1);
          }
          break;
        case "Ljakarta/validation/constraints/NotNull;":
          {
            schema.setNullable(false);
          }
          break;
        case "Ljakarta/validation/constraints/Null;":
          {
            schema.setNullable(true);
            var types = new LinkedHashSet<String>();
            Optional.ofNullable(schema.getType()).ifPresent(types::add);
            Optional.ofNullable(schema.getTypes()).ifPresent(types::addAll);
            types.add("null");
            schema.setTypes(types);
          }
          break;
        // Ignored
        // case "Ljakarta/validation/constraints/Past;":{}break;
        // case "Ljakarta/validation/constraints/PastOrPresent;":{}break;
        case "Ljakarta/validation/constraints/Pattern;":
          {
            AnnotationUtils.findAnnotationValue(annotation, "regexp")
                .map(String.class::cast)
                .ifPresent(schema::setPattern);
          }
          break;
        case "Ljakarta/validation/constraints/Positive;":
          {
            schema.setMinimum(BigDecimal.ONE);
          }
          break;
        case "Ljakarta/validation/constraints/PositiveOrZero;":
          {
            schema.setMinimum(BigDecimal.ZERO);
          }
          break;
        case "Ljakarta/validation/constraints/Size;":
          {
            AnnotationUtils.findAnnotationValue(annotation, "min")
                .map(Object::toString)
                .ifPresent(
                    value -> {
                      schema.setMinimum(new BigDecimal(value));
                    });
            AnnotationUtils.findAnnotationValue(annotation, "max")
                .map(Object::toString)
                .ifPresent(
                    value -> {
                      schema.setMaximum(new BigDecimal(value));
                    });
          }
          break;
      }
    }
  }
}
