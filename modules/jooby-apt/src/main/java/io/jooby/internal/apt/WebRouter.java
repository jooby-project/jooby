/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt;

import static io.jooby.internal.apt.AnnotationSupport.findAnnotationByName;
import static io.jooby.internal.apt.CodeBlock.indent;
import static io.jooby.internal.apt.CodeBlock.semicolon;

import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

public abstract class WebRouter<R extends WebRoute> {
  public static final String JAVA =
      """
      package ${packageName};
      ${imports}
      @io.jooby.annotation.Generated(${className}.class)
      public class ${generatedClassName} implements ${implements} {
          protected java.util.function.Function<io.jooby.Context, ${className}> factory;
      ${constructors}
          public ${generatedClassName}(${className} instance) {
             setup(ctx -> instance);
          }

          public ${generatedClassName}(io.jooby.SneakyThrows.Supplier<${className}> provider) {
             setup(ctx -> (${className}) provider.get());
          }

          public ${generatedClassName}(io.jooby.SneakyThrows.Function<Class<${className}>, ${className}> provider) {
             setup(ctx -> provider.apply(${className}.class));
          }

          private void setup(java.util.function.Function<io.jooby.Context, ${className}> factory) {
              this.factory = factory;
          }

      ${methods}
      }

      """;
  public static final String KOTLIN =
      """
      package ${packageName}
          ${imports}
          @io.jooby.annotation.Generated(${className}::class)
          open class ${generatedClassName} : ${implements} {
              private lateinit var factory: java.util.function.Function<io.jooby.Context, ${className}>

              ${constructors}
              constructor(instance: ${className}) { setup { instance } }

              constructor(provider: io.jooby.SneakyThrows.Supplier<${className}>) { setup { provider.get() } }

              constructor(provider: (Class<${className}>) -> ${className}) { setup { provider(${className}::class.java) } }

              constructor(provider:  io.jooby.SneakyThrows.Function<Class<${className}>, ${className}>) { setup { provider.apply(${className}::class.java) } }

              private fun setup(factory: java.util.function.Function<io.jooby.Context, ${className}>) {
                this.factory = factory
              }
          ${methods}
          }

      """;

  protected final MvcContext context;
  protected final TypeElement clazz;
  protected final Map<String, R> routes = new LinkedHashMap<>();

  public WebRouter(MvcContext context, TypeElement clazz) {
    this.context = context;
    this.clazz = clazz;
  }

  public abstract String getGeneratedType();

  public abstract String getSourceCode(Boolean generateKotlin) throws IOException;

  public String getGeneratedFilename() {
    return getGeneratedType().replace('.', '/') + (isKt() ? ".kt" : ".java");
  }

  public TypeElement getTargetType() {
    return clazz;
  }

  public List<R> getRoutes() {
    return new ArrayList<>(routes.values());
  }

  public boolean isEmpty() {
    return routes.isEmpty();
  }

  public boolean isAbstract() {
    return clazz.getModifiers().contains(Modifier.ABSTRACT);
  }

  public boolean isKt() {
    return context
        .getProcessingEnvironment()
        .getElementUtils()
        .getAllAnnotationMirrors(getTargetType())
        .stream()
        .anyMatch(it -> it.getAnnotationType().asElement().toString().equals("kotlin.Metadata"));
  }

  public String getPackageName() {
    var classname = getGeneratedType();
    var pkgEnd = classname.lastIndexOf('.');
    return pkgEnd > 0 ? classname.substring(0, pkgEnd) : "";
  }

  public boolean hasBeanValidation() {
    return getRoutes().stream().anyMatch(WebRoute::hasBeanValidation);
  }

  protected StringBuilder trimr(StringBuilder buffer) {
    var i = buffer.length() - 1;
    while (i > 0 && Character.isWhitespace(buffer.charAt(i))) {
      buffer.deleteCharAt(i);
      i = buffer.length() - 1;
    }
    return buffer;
  }

  protected StringBuilder constructors(String generatedName, boolean kt) {
    var constructors =
        getTargetType().getEnclosedElements().stream()
            .filter(
                it ->
                    it.getKind() == ElementKind.CONSTRUCTOR
                        && it.getModifiers().contains(Modifier.PUBLIC))
            .map(ExecutableElement.class::cast)
            .toList();
    var targetType = getTargetType().getSimpleName();
    var buffer = new StringBuilder();
    buffer.append(System.lineSeparator());
    // Inject could be at constructor or field level.
    var injectConstructor =
        constructors.stream().filter(hasInjectAnnotation()).findFirst().orElse(null);
    var inject = injectConstructor != null || hasInjectAnnotation(getTargetType());
    final var defaultConstructor =
        constructors.stream().filter(it -> it.getParameters().isEmpty()).findFirst().orElse(null);
    if (inject) {
      constructor(
          generatedName,
          kt,
          kt ? ":" : null,
          buffer,
          List.of(),
          (output, params) -> {
            output
                .append("this(")
                .append(targetType)
                .append(kt ? "::class" : ".class")
                .append(")")
                .append(semicolon(kt))
                .append(System.lineSeparator());
          });
    } else {
      if (defaultConstructor != null) {
        constructor(
            generatedName,
            kt,
            kt ? ":" : null,
            buffer,
            List.of(),
            (output, params) -> {
              if (kt) {
                output
                    .append("this(")
                    .append(targetType)
                    .append("())")
                    .append(semicolon(true))
                    .append(System.lineSeparator());
              } else {
                output
                    .append("this(")
                    .append("io.jooby.SneakyThrows.singleton(")
                    .append(targetType)
                    .append("::new))")
                    .append(semicolon(false))
                    .append(System.lineSeparator());
              }
            });
      }
    }
    var skip =
        Stream.of(injectConstructor, defaultConstructor)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    for (ExecutableElement constructor : constructors) {
      if (!skip.contains(constructor)) {
        constructor(
            generatedName,
            kt,
            kt ? ":" : null,
            buffer,
            constructor.getParameters().stream()
                .map(it -> Map.<Object, String>entry(it.asType(), it.getSimpleName().toString()))
                .toList(),
            (output, params) -> {
              var separator = ", ";
              output.append("this(").append(kt ? "" : "new ").append(targetType).append("(");
              params.forEach(e -> output.append(e.getValue()).append(separator));
              output.setLength(output.length() - separator.length());
              output.append("))").append(semicolon(kt)).append(System.lineSeparator());
            });
      }
    }

    if (inject) {
      if (kt) {
        constructor(
            generatedName,
            true,
            "{",
            buffer,
            List.of(Map.entry("kotlin.reflect.KClass<" + targetType + ">", "type")),
            (output, params) -> {
              output
                  .append("setup { ctx -> ctx.require<")
                  .append(targetType)
                  .append(">(type.java)")
                  .append(" }")
                  .append(System.lineSeparator());
            });
      } else {
        constructor(
            generatedName,
            false,
            null,
            buffer,
            List.of(Map.entry("Class<" + targetType + ">", "type")),
            (output, params) -> {
              output
                  .append("setup(")
                  .append("ctx -> ctx.require(type)")
                  .append(")")
                  .append(";")
                  .append(System.lineSeparator());
            });
      }
    }

    return trimr(buffer).append(System.lineSeparator());
  }

  private void constructor(
      String generatedName,
      boolean kt,
      String ktBody,
      StringBuilder buffer,
      List<Map.Entry<Object, String>> parameters,
      BiConsumer<StringBuilder, List<Map.Entry<Object, String>>> body) {
    buffer.append(indent(4));
    if (kt) {
      buffer.append("constructor").append("(");
    } else {
      buffer.append("public ").append(generatedName).append("(");
    }
    var separator = ", ";
    parameters.forEach(
        e -> {
          if (kt) {
            buffer.append(e.getValue()).append(": ").append(e.getKey()).append(separator);
          } else {
            buffer.append(e.getKey()).append(" ").append(e.getValue()).append(separator);
          }
        });
    if (!parameters.isEmpty()) {
      buffer.setLength(buffer.length() - separator.length());
    }
    buffer.append(")");
    if (!kt) {
      buffer.append(" {").append(System.lineSeparator());
      buffer.append(indent(6));
    } else {
      buffer.append(" ").append(ktBody).append(" ");
    }
    body.accept(buffer, parameters);
    if (!kt || "{".equals(ktBody)) {
      buffer.append(indent(4)).append("}");
    }
    buffer.append(System.lineSeparator()).append(System.lineSeparator());
  }

  private boolean hasInjectAnnotation(TypeElement targetClass) {
    var inject = false;
    while (!inject && !targetClass.toString().equals("java.lang.Object")) {
      inject = targetClass.getEnclosedElements().stream().anyMatch(hasInjectAnnotation());
      targetClass =
          (TypeElement)
              context
                  .getProcessingEnvironment()
                  .getTypeUtils()
                  .asElement(targetClass.getSuperclass());
    }
    return inject;
  }

  private static Predicate<Element> hasInjectAnnotation() {
    var injectAnnotations =
        Set.of("javax.inject.Inject", "jakarta.inject.Inject", "com.google.inject.Inject");
    return it ->
        injectAnnotations.stream()
            .anyMatch(annotation -> findAnnotationByName(it, annotation) != null);
  }
}
