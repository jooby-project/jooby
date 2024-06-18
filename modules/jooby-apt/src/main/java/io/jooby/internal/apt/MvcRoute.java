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

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;

import io.jooby.apt.MvcContext;

public class MvcRoute {
  private final MvcContext context;
  private final MvcRouter router;
  private final ExecutableElement method;
  private final Map<TypeElement, AnnotationMirror> annotationMap = new LinkedHashMap<>();
  private final List<MvcParameter> parameters;
  private final TypeDefinition returnType;
  private String generatedName;
  private final boolean suspendFun;

  public MvcRoute(MvcContext context, MvcRouter router, ExecutableElement method) {
    this.context = context;
    this.router = router;
    this.method = method;
    this.parameters =
        method.getParameters().stream().map(it -> new MvcParameter(context, it)).toList();
    this.suspendFun =
        !parameters.isEmpty()
            && parameters.get(parameters.size() - 1).getType().is("kotlin.coroutines.Continuation");
    this.returnType =
        new TypeDefinition(
            context.getProcessingEnvironment().getTypeUtils(), method.getReturnType());
  }

  public MvcRoute(MvcContext context, MvcRouter router, MvcRoute route) {
    this.context = context;
    this.router = router;
    this.method = route.method;
    this.parameters =
        method.getParameters().stream().map(it -> new MvcParameter(context, it)).toList();
    this.returnType =
        new TypeDefinition(
            context.getProcessingEnvironment().getTypeUtils(), method.getReturnType());
    this.suspendFun = route.suspendFun;
    route.annotationMap.keySet().forEach(this::addHttpMethod);
  }

  public TypeDefinition getReturnType() {
    var processingEnv = context.getProcessingEnvironment();
    var types = processingEnv.getTypeUtils();
    var elements = processingEnv.getElementUtils();
    if (returnType.isVoid()) {
      return new TypeDefinition(types, elements.getTypeElement("io.jooby.StatusCode").asType());
    } else if (isSuspendFun()) {
      var continuation = parameters.get(parameters.size() - 1).getType();
      if (!continuation.getArguments().isEmpty()) {
        var continuationReturnType = continuation.getArguments().get(0).getType();
        if (continuationReturnType instanceof WildcardType wildcardType) {
          return Stream.of(wildcardType.getSuperBound(), wildcardType.getExtendsBound())
              .filter(Objects::nonNull)
              .findFirst()
              .map(e -> new TypeDefinition(types, e))
              .orElseGet(() -> new TypeDefinition(types, continuationReturnType));
        } else {
          return new TypeDefinition(types, continuationReturnType);
        }
      }
    }
    return returnType;
  }

  public TypeMirror getReturnTypeHandler() {
    return getReturnType().getRawType();
  }

  public List<String> generateMapping(boolean kt) {
    List<String> block = new ArrayList<>();
    var methodName = getGeneratedName();
    var returnType = getReturnType();
    var paramString = String.join(", ", getJavaMethodSignature(kt));
    var javadocLink = javadocComment();
    var attributeGenerator = new RouteAttributesGenerator(context);
    var routes = router.getRoutes();
    var lastRoute = routes.get(routes.size() - 1).equals(this);
    var entries = annotationMap.entrySet().stream().toList();
    var javaChainPrefix = kt ? "" : ".";
    var thisRef =
        isSuspendFun()
            ? "this@"
                + context.generateRouterName(router.getTargetType().getSimpleName().toString())
                + "::"
            : "this::";
    for (var e : entries) {
      var lastHttpMethod = lastRoute && entries.get(entries.size() - 1).equals(e);
      var annotation = e.getKey();
      var httpMethod = HttpMethod.findByAnnotationName(annotation.getQualifiedName().toString());
      var paths = context.path(router.getTargetType(), method, annotation);
      for (var path : paths) {
        var lastLine = lastHttpMethod && paths.get(paths.size() - 1).equals(path);
        block.add(javadocLink);
        block.add(
            statement(
                isSuspendFun() ? "" : "app.",
                annotation.getSimpleName().toString().toLowerCase(),
                "(",
                string(path),
                ", ",
                context.pipeline(getReturnTypeHandler(), thisRef + methodName),
                ")",
                kt ? ".apply {" : ""));
        /* consumes */
        mediaType(httpMethod::consumes)
            .ifPresent(
                consumes ->
                    block.add(
                        statement(indent(2), javaChainPrefix, "setConsumes(", consumes, ")")));
        /* produces */
        mediaType(httpMethod::produces)
            .ifPresent(
                produces ->
                    block.add(
                        statement(indent(2), javaChainPrefix, "setProduces(", produces, ")")));
        /* dispatch */
        dispatch()
            .ifPresent(
                dispatch ->
                    block.add(
                        statement(
                            indent(2), javaChainPrefix, "setExecutorKey(", string(dispatch), ")")));
        /* attributes */
        attributeGenerator
            .toSourceCode(this, indent(2))
            .ifPresent(
                attributes ->
                    block.add(
                        statement(indent(2), javaChainPrefix, "setAttributes(", attributes, ")")));
        /* returnType */
        block.add(
            statement(
                indent(2), javaChainPrefix, "setReturnType(", returnType.toSourceCode(kt), ")"));
        /* mvcMethod */
        var lineSep = lastLine ? lineSeparator() : lineSeparator() + lineSeparator();
        block.add(
            CodeBlock.of(
                indent(2),
                javaChainPrefix,
                "setMvcMethod(",
                router.getTargetType().getSimpleName(),
                clazz(kt),
                ".getMethod(",
                string(getMethodName()),
                paramString.isEmpty() ? "" : ", " + paramString,
                "))",
                semicolon(kt),
                lineSep));
        if (kt) {
          block.add("}");
        }
      }
    }
    return block;
  }

  public List<String> generateHandlerCall(boolean kt) {
    var buffer = new ArrayList<String>();
    /* Parameters */
    var paramList = new StringJoiner(", ", "(", ")");
    for (var parameter : getParameters(true)) {
      paramList.add(parameter.generateMapping(kt).toString());
    }
    var returnTypeString = type(kt, getReturnType().toString());
    var ctx = "ctx";
    if (kt) {
      buffer.add(statement("@Throws(Exception::class)"));
      if (isSuspendFun()) {
        buffer.add(
            statement(
                "suspend ",
                "fun ",
                getGeneratedName(),
                "(handler: io.jooby.kt.HandlerContext): ",
                returnTypeString,
                " {"));
        buffer.add(statement(indent(2), "val ctx = handler.ctx"));
      } else {
        buffer.add(
            statement(
                "fun ", getGeneratedName(), "(ctx: io.jooby.Context): ", returnTypeString, " {"));
      }
    } else {
      buffer.add(
          statement(
              "public ",
              returnTypeString,
              " ",
              getGeneratedName(),
              "(io.jooby.Context ctx) throws Exception {"));
    }
    if (returnType.isVoid()) {
      buffer.add(
          statement(
              indent(2),
              "ctx.setResponseCode(",
              ctx,
              ".getRoute().getMethod().equals(",
              string("DELETE"),
              ") ?" + " io.jooby.StatusCode.NO_CONTENT: io.jooby.StatusCode.OK)",
              semicolon(kt)));
      buffer.add(
          statement(
              indent(2),
              "this.factory.apply(",
              ctx,
              ").",
              this.method.getSimpleName(),
              paramList.toString(),
              semicolon(kt)));
      buffer.add(statement("return ", ctx, ".getResponseCode()", semicolon(kt)));
    } else if (returnType.is("io.jooby.StatusCode")) {
      buffer.add(
          statement(
              indent(2),
              isSuspendFun() ? "val" : "var",
              " statusCode = this.factory.apply(",
              ctx,
              ").",
              this.method.getSimpleName(),
              paramList.toString(),
              semicolon(kt)));
      buffer.add(statement(indent(2), ctx, ".setResponseCode(statusCode)", semicolon(kt)));
      buffer.add(statement(indent(2), "return statusCode", semicolon(kt)));
    } else {
      buffer.add(
          statement(
              indent(2),
              "return this.factory.apply(",
              ctx,
              ").",
              this.method.getSimpleName(),
              paramList.toString(),
              semicolon(kt)));
    }
    buffer.add(statement("}", System.lineSeparator()));
    return buffer;
  }

  public String getGeneratedName() {
    return generatedName;
  }

  public void setGeneratedName(String generatedName) {
    this.generatedName = generatedName;
  }

  public MvcRoute addHttpMethod(TypeElement annotation) {
    var annotationMirror =
        ofNullable(findAnnotationByName(this.method, annotation.getQualifiedName().toString()))
            .orElseThrow(() -> new IllegalArgumentException("Annotation not found: " + annotation));
    annotationMap.put(annotation, annotationMirror);
    return this;
  }

  public MvcRouter getRouter() {
    return router;
  }

  public List<MvcParameter> getParameters(boolean skipCoroutine) {
    return parameters.stream()
        .filter(type -> !skipCoroutine || !type.getType().is("kotlin.coroutines.Continuation"))
        .toList();
  }

  public ExecutableElement getMethod() {
    return method;
  }

  public List<String> getRawParameterTypes(boolean skipCoroutine) {
    return getParameters(skipCoroutine).stream()
        .map(MvcParameter::getType)
        .map(TypeDefinition::getRawType)
        .map(TypeMirror::toString)
        .map(it -> type(router.isKt(), it))
        .toList();
  }

  public List<String> getJavaMethodSignature(boolean kt) {
    return getParameters(false).stream()
        .map(
            it -> {
              var type = it.getType();
              // Kotlin requires his own types for primitives
              if (kt && type.isPrimitive()) {
                return type(kt, type.getRawType().toString());
              }
              return type.getRawType().toString();
            })
        .map(it -> it + clazz(kt))
        .toList();
  }

  public String getMethodName() {
    return getMethod().getSimpleName().toString();
  }

  @Override
  public int hashCode() {
    return method.toString().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof MvcRoute that) {
      return this.method.toString().equals(that.method.toString());
    }
    return false;
  }

  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder();
    for (var e : annotationMap.entrySet()) {
      var attributes = e.getValue().getElementValues();
      buffer.append("@").append(e.getKey().getSimpleName()).append("(");
      if (attributes.size() == 1) {
        buffer.append(attributes.values().iterator().next().getValue());
      } else {
        buffer.append(attributes);
      }
      buffer.append(") ");
    }
    buffer
        .append(method.getSimpleName())
        .append("(")
        .append(String.join(", ", getRawParameterTypes(true)))
        .append("): ")
        .append(getReturnType());
    return buffer.toString();
  }

  private Optional<String> dispatch() {
    var dispatch = dispatch(method);
    return dispatch.isEmpty() ? dispatch(router.getTargetType()) : dispatch;
  }

  private Optional<String> dispatch(Element element) {
    return ofNullable(findAnnotationByName(element, "io.jooby.annotation.Dispatch"))
        .map(it -> findAnnotationValue(it, VALUE).stream().findFirst().orElse("worker"));
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

  /**
   * Kotlin suspend function has a <code>kotlin.coroutines.Continuation</code> as last parameter.
   *
   * @return True for Kotlin suspend function.
   */
  public boolean isSuspendFun() {
    return suspendFun;
  }

  private String javadocComment() {
    return CodeBlock.of(
        "/* See {@link ",
        router.getTargetType().getSimpleName(),
        "#",
        getMethodName(),
        "(",
        String.join(", ", getRawParameterTypes(true)),
        ") */\n");
  }
}
