/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt;

import static io.jooby.internal.apt.AnnotationSupport.*;
import static io.jooby.internal.apt.CodeBlock.*;
import static java.lang.System.lineSeparator;
import static java.util.Optional.ofNullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;

public class RestRoute extends WebRoute<RestRouter> {
  private final TypeElement httpMethodAnnotation;
  private String generatedName;

  public RestRoute(RestRouter router, ExecutableElement method, TypeElement httpMethodAnnotation) {
    super(router, method);
    this.httpMethodAnnotation = httpMethodAnnotation;
    this.generatedName = method.getSimpleName().toString();
  }

  public String getGeneratedName() {
    return generatedName;
  }

  public void setGeneratedName(String generatedName) {
    this.generatedName = generatedName;
  }

  private String methodReference(boolean kt, String thisRef, String methodName) {
    if (kt) {
      var generics = returnType.getArgumentsString(kt, true, Set.of(TypeKind.TYPEVAR));
      if (!generics.isEmpty()) {
        return CodeBlock.of(") { ctx -> ", methodName, generics, "(ctx) }");
      }
    }
    return thisRef + methodName + ")";
  }

  private Optional<String> dispatch() {
    var dispatch = dispatch(method);
    return dispatch.isEmpty() ? dispatch(router.getTargetType()) : dispatch;
  }

  private Optional<String> dispatch(Element element) {
    return ofNullable(findAnnotationByName(element, "io.jooby.annotation.Dispatch"))
        .map(
            it ->
                findAnnotationValue(it, AnnotationSupport.VALUE).stream()
                    .findFirst()
                    .orElse("worker"));
  }

  private Optional<String> mediaType(Function<Element, List<String>> lookup) {
    var scopes = List.of(method, router.getTargetType());
    var i = 0;
    var types = Collections.<String>emptyList();
    while (types.isEmpty() && i < scopes.size()) {
      types = lookup.apply(scopes.get(i++));
    }
    if (types.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(
        types.stream()
            .map(type -> CodeBlock.of("io.jooby.MediaType.valueOf(", string(type), ")"))
            .collect(Collectors.joining(", ", "java.util.List.of(", ")")));
  }

  private String javadocComment(boolean kt, String routerName) {
    if (kt) {
      return CodeBlock.statement("/** See [", routerName, ".", getMethodName(), "]", " */");
    }
    return CodeBlock.statement(
        "/** See {@link ",
        routerName,
        "#",
        getMethodName(),
        "(",
        String.join(", ", getRawParameterTypes(true, false)),
        ") */");
  }

  public List<String> generateMapping(boolean kt, String routerName, boolean isLastRoute) {
    List<String> block = new ArrayList<>();
    var methodName = getGeneratedName();
    var returnType = getReturnType();
    var paramString = String.join(", ", getJavaMethodSignature(kt));
    var javadocLink = javadocComment(kt, routerName);
    var attributeGenerator = new RouteAttributesGenerator(context, hasBeanValidation);

    var httpMethod =
        HttpMethod.findByAnnotationName(httpMethodAnnotation.getQualifiedName().toString());
    var dslMethod = httpMethodAnnotation.getSimpleName().toString().toLowerCase();
    var paths = context.path(router.getTargetType(), method, httpMethodAnnotation);

    var thisRef =
        isSuspendFun() ? "this@" + context.generateRouterName(routerName) + "::" : "this::";

    for (var path : paths) {
      var lastLine = isLastRoute && paths.get(paths.size() - 1).equals(path);
      block.add(javadocLink);
      block.add(
          statement(
              isSuspendFun() ? "" : "app.",
              dslMethod,
              "(",
              string(leadingSlash(path)),
              ", ",
              context.pipeline(
                  getReturnType().getRawType(), methodReference(kt, thisRef, methodName))));

      if (context.nonBlocking(getReturnType().getRawType()) || isSuspendFun()) {
        block.add(statement(indent(2), ".setNonBlocking(true)"));
      }
      mediaType(httpMethod::consumes)
          .ifPresent(consumes -> block.add(statement(indent(2), ".setConsumes(", consumes, ")")));
      mediaType(httpMethod::produces)
          .ifPresent(produces -> block.add(statement(indent(2), ".setProduces(", produces, ")")));
      dispatch()
          .ifPresent(
              dispatch ->
                  block.add(statement(indent(2), ".setExecutorKey(", string(dispatch), ")")));

      attributeGenerator
          .toSourceCode(kt, this, 2)
          .ifPresent(
              attributes -> block.add(statement(indent(2), ".setAttributes(", attributes, ")")));

      var lineSep = lastLine ? lineSeparator() : lineSeparator() + lineSeparator();

      if (context.generateMvcMethod()) {
        block.add(
            CodeBlock.of(
                indent(2),
                ".setMvcMethod(",
                kt ? "" : "new ",
                "io.jooby.Route.MvcMethod(",
                routerName,
                clazz(kt),
                ", ",
                string(getMethodName()),
                ", ",
                type(kt, returnType.getRawType().toString()),
                clazz(kt),
                paramString.isEmpty() ? "" : ", " + paramString,
                "))",
                semicolon(kt),
                lineSep));
      } else {
        var lastStatement = block.getLast();
        if (lastStatement.endsWith(lineSeparator())) {
          lastStatement =
              lastStatement.substring(0, lastStatement.length() - lineSeparator().length());
        }
        block.set(block.size() - 1, lastStatement + semicolon(kt) + lineSep);
      }
    }
    return block;
  }

  public List<String> generateHandlerCall(boolean kt) {
    var buffer = new ArrayList<String>();
    var methodName = getGeneratedName();
    var paramList = new StringJoiner(", ", "(", ")");

    var customReturnType = getReturnType();
    var returnTypeGenerics =
        customReturnType.getArgumentsString(kt, false, Set.of(TypeKind.TYPEVAR));
    var returnTypeString = type(kt, customReturnType.toString());

    String projection = getProjection();

    boolean isProjectedReturnType =
        customReturnType.isProjection() || customReturnType.is(Types.PROJECTED);

    // Create separate variables for the generated HTTP handler's signature
    var handlerTypeGenerics = returnTypeGenerics;
    var handlerTypeString = returnTypeString;

    // ONLY modify the signature if we need to wrap a NON-projected type
    if (projection != null && !isProjectedReturnType) {
      handlerTypeGenerics = "";
      handlerTypeString = Types.PROJECTED + "<" + returnTypeString + ">";
    }

    methodCallHeader(
        kt,
        methodName,
        buffer,
        handlerTypeGenerics,
        handlerTypeString,
        !method.getThrownTypes().isEmpty());

    int controllerIndent = 2;

    for (var parameter : getParameters(true)) {
      var generatedParameter = parameter.generateMapping(kt);
      if (parameter.isRequireBeanValidation()) {
        generatedParameter =
            CodeBlock.of(
                "io.jooby.validation.BeanValidator.apply(", "ctx, ", generatedParameter, ")");
      }
      paramList.add(generatedParameter);
    }

    buffer.add(
        statement(indent(controllerIndent), var(kt), "c = this.factory.apply(ctx)", semicolon(kt)));

    if (returnType.isVoid()) {
      String statusCode =
          httpMethodAnnotation.getSimpleName().toString().equals("DELETE") ? "NO_CONTENT" : "OK";
      buffer.add(
          statement(
              indent(controllerIndent),
              "ctx.setResponseCode(io.jooby.StatusCode.",
              statusCode,
              ")",
              semicolon(kt)));

      String call = makeCall(kt, paramList.toString(), false, false);

      buffer.add(statement(indent(controllerIndent), call, semicolon(kt)));
      buffer.add(
          statement(indent(controllerIndent), "return ctx.getResponseCode()", semicolon(kt)));
    } else if (returnType.is("io.jooby.StatusCode")) {
      var call = makeCall(kt, paramList.toString(), false, false);

      buffer.add(
          statement(
              indent(controllerIndent), kt ? "val" : "var", " statusCode = ", call, semicolon(kt)));
      buffer.add(
          statement(indent(controllerIndent), "ctx.setResponseCode(statusCode)", semicolon(kt)));
      buffer.add(statement(indent(controllerIndent), "return statusCode", semicolon(kt)));
    } else {
      var call = makeCall(kt, paramList.toString(), isProjectedReturnType, isProjectedReturnType);
      var nullable = kt && isNullableKotlinReturn();

      // ONLY wrap the call if it's a NON-projected type with a projection string
      if (projection != null && !isProjectedReturnType) {
        var projected = of(Types.PROJECTED, ".wrap(", call, ").include(", string(projection), ")");
        buffer.add(
            statement(
                indent(controllerIndent),
                "return ",
                projected,
                nullable ? "!!" : "",
                semicolon(kt)));
      } else {
        buffer.add(
            statement(
                indent(controllerIndent), "return ", call, nullable ? "!!" : "", semicolon(kt)));
      }
    }

    buffer.add(statement("}", lineSeparator()));

    if (isUncheckedCast()) {
      if (kt) {
        buffer.addFirst(statement("@Suppress(\"UNCHECKED_CAST\")"));
      } else {
        buffer.addFirst(statement("@SuppressWarnings(\"unchecked\")"));
      }
    }

    return buffer;
  }

  private void methodCallHeader(
      boolean kt,
      String methodName,
      ArrayList<String> buffer,
      String returnTypeGenerics,
      String returnTypeString,
      boolean throwsException) {
    if (kt) {
      if (throwsException) buffer.add(statement("@Throws(Exception::class)"));

      if (isSuspendFun()) {
        buffer.add(
            statement(
                "suspend ",
                "fun ",
                returnTypeGenerics,
                methodName,
                "(handler: io.jooby.kt.HandlerContext): ",
                returnTypeString,
                " {"));
        buffer.add(statement(indent(2), "val ctx = handler.ctx"));
      } else {
        buffer.add(
            statement(
                "fun ",
                returnTypeGenerics,
                methodName,
                "(ctx: io.jooby.Context): ",
                returnTypeString,
                " {"));
      }
    } else {
      buffer.add(
          statement(
              "public ",
              returnTypeGenerics,
              returnTypeString,
              " ",
              methodName,
              "(io.jooby.Context ctx)",
              throwsException ? "throws Exception {" : "{"));
    }
  }

  private String getProjection() {
    var project = AnnotationSupport.findAnnotationByName(method, Types.PROJECT);
    if (project != null) {
      return AnnotationSupport.findAnnotationValue(project, VALUE).stream()
          .findFirst()
          .orElse(null);
    }

    // Get the annotation mirror from the method, not the annotation type definition
    var httpMethodMirror =
        AnnotationSupport.findAnnotationByName(
            method, httpMethodAnnotation.getQualifiedName().toString());
    if (httpMethodMirror != null) {
      var projection =
          AnnotationSupport.findAnnotationValue(httpMethodMirror, "projection"::equals);
      return projection.stream().findFirst().orElse(null);
    }
    return null;
  }
}
