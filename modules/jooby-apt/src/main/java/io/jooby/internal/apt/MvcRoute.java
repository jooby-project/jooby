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
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;

public class MvcRoute {
  private final MvcContext context;
  private final MvcRouter router;
  private final ExecutableElement method;
  private final Map<TypeElement, AnnotationMirror> annotationMap = new LinkedHashMap<>();
  private final List<MvcParameter> parameters;
  private final TypeDefinition returnType;
  private String generatedName;
  private final boolean suspendFun;
  private boolean uncheckedCast;
  private final boolean hasBeanValidation;

  public MvcRoute(MvcContext context, MvcRouter router, ExecutableElement method) {
    this.context = context;
    this.router = router;
    this.method = method;
    this.parameters =
        method.getParameters().stream().map(it -> new MvcParameter(context, this, it)).toList();
    this.hasBeanValidation = parameters.stream().anyMatch(MvcParameter::isRequireBeanValidation);
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
        method.getParameters().stream().map(it -> new MvcParameter(context, this, it)).toList();
    this.hasBeanValidation = parameters.stream().anyMatch(MvcParameter::isRequireBeanValidation);
    this.returnType =
        new TypeDefinition(
            context.getProcessingEnvironment().getTypeUtils(), method.getReturnType());
    this.suspendFun = route.suspendFun;
    route.annotationMap.keySet().forEach(this::addHttpMethod);
  }

  public MvcContext getContext() {
    return context;
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
    var javadocLink = javadocComment(kt);
    var attributeGenerator = new RouteAttributesGenerator(context, hasBeanValidation);
    var routes = router.getRoutes();
    var lastRoute = routes.get(routes.size() - 1).equals(this);
    var entries = annotationMap.entrySet().stream().toList();
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
                string(leadingSlash(path)),
                ", ",
                context.pipeline(
                    getReturnTypeHandler(), methodReference(kt, thisRef, methodName))));
        if (context.nonBlocking(getReturnTypeHandler()) || isSuspendFun()) {
          block.add(statement(indent(2), ".setNonBlocking(true)"));
        }
        /* consumes */
        mediaType(httpMethod::consumes)
            .ifPresent(consumes -> block.add(statement(indent(2), ".setConsumes(", consumes, ")")));
        /* produces */
        mediaType(httpMethod::produces)
            .ifPresent(produces -> block.add(statement(indent(2), ".setProduces(", produces, ")")));
        /* dispatch */
        dispatch()
            .ifPresent(
                dispatch ->
                    block.add(statement(indent(2), ".setExecutorKey(", string(dispatch), ")")));
        /* attributes */
        attributeGenerator
            .toSourceCode(kt, this, 2)
            .ifPresent(
                attributes -> block.add(statement(indent(2), ".setAttributes(", attributes, ")")));
        var lineSep = lastLine ? lineSeparator() : lineSeparator() + lineSeparator();
        if (context.generateMvcMethod()) {
          /* mvcMethod */
          block.add(
              CodeBlock.of(
                  indent(2),
                  ".setMvcMethod(",
                  kt ? "" : "new ",
                  "io.jooby.Route.MvcMethod(",
                  router.getTargetType().getSimpleName().toString(),
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
          var lastStatement = block.get(block.size() - 1);
          if (lastStatement.endsWith(lineSeparator())) {
            lastStatement =
                lastStatement.substring(0, lastStatement.length() - lineSeparator().length());
          }
          block.set(block.size() - 1, lastStatement + semicolon(kt) + lineSep);
        }
      }
    }
    return block;
  }

  private String methodReference(boolean kt, String thisRef, String methodName) {
    if (kt) {
      var returnType = getReturnType();
      var generics = returnType.getArgumentsString(kt, true, Set.of(TypeKind.TYPEVAR));
      if (!generics.isEmpty()) {
        return CodeBlock.of(") { ctx -> ", methodName, generics, "(ctx) }");
      }
    }
    return thisRef + methodName + ")";
  }

  /**
   * Ensure path start with a <code>/</code>(leading slash).
   *
   * @param path Path to process.
   * @return Path with leading slash.
   */
  static String leadingSlash(String path) {
    if (path == null || path.isEmpty() || path.equals("/")) {
      return "/";
    }
    return path.charAt(0) == '/' ? path : "/" + path;
  }

  public List<String> generateHandlerCall(boolean kt) {
    var buffer = new ArrayList<String>();
    /* Parameters */
    var paramList = new StringJoiner(", ", "(", ")");
    for (var parameter : getParameters(true)) {
      String generatedParameter = parameter.generateMapping(kt);
      if (parameter.isRequireBeanValidation()) {
        generatedParameter =
            CodeBlock.of(
                "io.jooby.validation.BeanValidator.apply(", "ctx, ", generatedParameter, ")");
      }

      paramList.add(generatedParameter);
    }
    var throwsException = !method.getThrownTypes().isEmpty();
    var returnTypeGenerics =
        getReturnType().getArgumentsString(kt, false, Set.of(TypeKind.TYPEVAR));
    var returnTypeString = type(kt, getReturnType().toString());
    boolean nullable = false;
    if (kt) {
      nullable =
          method.getAnnotationMirrors().stream()
              .map(AnnotationMirror::getAnnotationType)
              .map(Objects::toString)
              .anyMatch(NULLABLE);
      if (throwsException) {
        buffer.add(statement("@Throws(Exception::class)"));
      }
      if (isSuspendFun()) {
        buffer.add(
            statement(
                "suspend ",
                "fun ",
                returnTypeGenerics,
                getGeneratedName(),
                "(handler: io.jooby.kt.HandlerContext): ",
                returnTypeString,
                " {"));
        buffer.add(statement(indent(2), "val ctx = handler.ctx"));
      } else {
        buffer.add(
            statement(
                "fun ",
                returnTypeGenerics,
                getGeneratedName(),
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
              getGeneratedName(),
              "(io.jooby.Context ctx) ",
              throwsException ? "throws Exception {" : "{"));
    }
    if (returnType.isVoid()) {
      String statusCode;
      if (annotationMap.size() == 1) {
        statusCode =
            annotationMap.keySet().iterator().next().getSimpleName().toString().equals("DELETE")
                ? "NO_CONTENT"
                : "OK";
      } else {
        statusCode = null;
      }
      if (statusCode != null) {
        buffer.add(
            statement(
                indent(2),
                "ctx.setResponseCode(io.jooby.StatusCode.",
                statusCode,
                ")",
                semicolon(kt)));
      } else {
        if (kt) {
          buffer.add(
              statement(
                  indent(2),
                  "ctx.setResponseCode(if (ctx.getRoute().getMethod().equals(",
                  string("DELETE"),
                  ")) io.jooby.StatusCode.NO_CONTENT else io.jooby.StatusCode.OK)"));
        } else {
          buffer.add(
              statement(
                  indent(2),
                  "ctx.setResponseCode(ctx.getRoute().getMethod().equals(",
                  string("DELETE"),
                  ") ? io.jooby.StatusCode.NO_CONTENT: io.jooby.StatusCode.OK)",
                  semicolon(false)));
        }
      }
      controllerVar(kt, buffer);
      buffer.add(
          statement(
              indent(2), "c.", this.method.getSimpleName(), paramList.toString(), semicolon(kt)));
      buffer.add(statement(indent(2), "return ctx.getResponseCode()", semicolon(kt)));
    } else if (returnType.is("io.jooby.StatusCode")) {
      controllerVar(kt, buffer);
      buffer.add(
          statement(
              indent(2),
              kt ? "val" : "var",
              " statusCode = c.",
              this.method.getSimpleName(),
              paramList.toString(),
              semicolon(kt)));
      buffer.add(statement(indent(2), "ctx.setResponseCode(statusCode)", semicolon(kt)));
      buffer.add(statement(indent(2), "return statusCode", semicolon(kt)));
    } else {
      controllerVar(kt, buffer);
      var cast = getReturnType().getArgumentsString(kt, false, Set.of(TypeKind.TYPEVAR));
      var kotlinNotEnoughTypeInformation = !cast.isEmpty() && kt ? "<Any>" : "";
      var call =
          of(
              "c.",
              this.method.getSimpleName(),
              kotlinNotEnoughTypeInformation,
              paramList.toString());
      if (!cast.isEmpty()) {
        setUncheckedCast(true);
        call = kt ? call + " as " + returnTypeString : "(" + returnTypeString + ") " + call;
      }
      buffer.add(statement(indent(2), "return ", call, kt && nullable ? "!!" : "", semicolon(kt)));
    }
    buffer.add(statement("}", System.lineSeparator()));
    if (uncheckedCast) {
      if (kt) {
        buffer.add(0, statement("@Suppress(\"UNCHECKED_CAST\")"));
      } else {
        buffer.add(0, statement("@SuppressWarnings(\"unchecked\")"));
      }
    }
    return buffer;
  }

  private void controllerVar(boolean kt, List<String> buffer) {
    buffer.add(
        statement(indent(2), kt ? "val" : "var", " c = this.factory.apply(ctx)", semicolon(kt)));
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

  private String javadocComment(boolean kt) {
    if (kt) {
      return CodeBlock.statement(
          "/** See [", router.getTargetType().getSimpleName(), ".", getMethodName(), "]", " */");
    }
    return CodeBlock.statement(
        "/** See {@link ",
        router.getTargetType().getSimpleName(),
        "#",
        getMethodName(),
        "(",
        String.join(", ", getRawParameterTypes(true)),
        ") */");
  }

  public void setUncheckedCast(boolean value) {
    this.uncheckedCast = value;
  }

  public boolean hasBeanValidation() {
    return hasBeanValidation;
  }
}
