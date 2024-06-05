/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt;

import static io.jooby.internal.apt.AnnotationSupport.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import io.jooby.apt.MvcContext;

public class MvcRoute {
  private final MvcContext context;
  private final MvcRouter router;
  private final ExecutableElement method;
  private final Map<TypeElement, AnnotationMirror> annotationMap = new LinkedHashMap<>();
  private final List<MvcParameter> parameters;
  private final TypeDefinition returnType;
  private String generatedName;

  public MvcRoute(MvcContext context, MvcRouter router, ExecutableElement method) {
    this.context = context;
    this.router = router;
    this.method = method;
    this.parameters =
        method.getParameters().stream().map(it -> new MvcParameter(context, it)).toList();
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
    route.annotationMap.keySet().forEach(this::addHttpMethod);
  }

  public TypeDefinition getReturnType() {
    var processingEnv = context.getProcessingEnvironment();
    var types = processingEnv.getTypeUtils();
    var elements = processingEnv.getElementUtils();
    if (returnType.isVoid()) {
      return new TypeDefinition(types, elements.getTypeElement("io.jooby.StatusCode").asType());
    }
    return returnType;
  }

  public List<CodeBlock> generateMapping() {
    List<CodeBlock> block = new ArrayList<>();
    var methodName = getGeneratedName();
    var isSuspend = isSuspendFun();
    var returnType = getReturnType();
    var paramTypes = getRawParameterTypes();
    var paramString =
        paramTypes.stream().map(it -> it + ".class").collect(Collectors.joining(", "));
    var javadocLink = javadocComment(paramTypes);
    var attributeGenerator = new RouteAttributesGenerator(context);
    var suspendPrefix = isSuspend ? "new io.jooby.kt.CoroutineHandler(" : "";
    var suspendSuffix = isSuspend ? ")" : "";
    for (var e : annotationMap.entrySet()) {
      var annotation = e.getKey();
      var httpMethod = HttpMethod.findByAnnotationName(annotation.getQualifiedName().toString());
      var paths = context.path(router.getTargetType(), method, annotation);
      for (var path : paths) {
        block.add(javadocLink);
        block.add(
            CodeBlock.of(
                "app.$L($S, $L$L::$L$L)\n",
                annotation.getSimpleName().toString().toLowerCase(),
                path,
                suspendPrefix,
                "this",
                methodName,
                suspendSuffix));
        /* consumes */
        mediaType(httpMethod::consumes)
            .ifPresent(consumes -> block.add(CodeBlock.of("   .setConsumes($L)\n", consumes)));
        /* produces */
        mediaType(httpMethod::produces)
            .ifPresent(produces -> block.add(CodeBlock.of("   .setProduces($L)\n", produces)));
        /* dispatch */
        dispatch()
            .ifPresent(dispatch -> block.add(CodeBlock.of("   .setExecutorKey($S)\n", dispatch)));
        /* attributes */
        attributeGenerator
            .toSourceCode(this, "   ")
            .ifPresent(
                attributes -> block.add(CodeBlock.of("   .setAttributes($L)\n", attributes)));
        /* returnType */
        block.add(CodeBlock.of("   .setReturnType($L)\n", returnType.toSourceCode()));
        /* mvcMethod */
        block.add(
            CodeBlock.of(
                "   .setMvcMethod($L.class.getMethod($S$L));\n\n",
                router.getTargetType().getSimpleName(),
                getMethodName(),
                paramString.isEmpty() ? "" : ", " + paramString));
      }
    }
    return block;
  }

  public MethodSpec generateHandlerCall() {
    var environment = context.getProcessingEnvironment();
    var elements = environment.getElementUtils();
    var contextType = TypeName.get(elements.getTypeElement("io.jooby.Context").asType());
    var methodSpec = MethodSpec.methodBuilder(getGeneratedName());
    methodSpec.addModifiers(Modifier.PUBLIC);
    methodSpec.addParameter(ParameterSpec.builder(contextType, "ctx").build());
    methodSpec.addException(Exception.class);
    /* Parameters */
    var paramList = new StringJoiner(", ", "(", ")");
    for (var parameter : getParameters()) {
      paramList.add(parameter.generateMapping().toString());
    }
    if (returnType.isVoid()) {
      methodSpec.addStatement(
          "ctx.setResponseCode(ctx.getRoute().getMethod().equals($S) ?"
              + " io.jooby.StatusCode.NO_CONTENT: io.jooby.StatusCode.OK)",
          "DELETE");
      methodSpec.addStatement(
          "this.provider.apply(ctx).$L$L", this.method.getSimpleName(), paramList);
      methodSpec.addStatement("return ctx.getResponseCode()");
    } else if (returnType.is("io.jooby.StatusCode")) {
      methodSpec.addStatement(
          "var statusCode = this.provider.apply(ctx).$L$L", this.method.getSimpleName(), paramList);
      methodSpec.addStatement("ctx.setResponseCode(statusCode)");
      methodSpec.addStatement("return statusCode");
    } else {
      methodSpec.addStatement(
          "return this.provider.apply(ctx).$L$L", this.method.getSimpleName(), paramList);
    }

    methodSpec.returns(TypeName.get(getReturnType().getType()));
    return methodSpec.build();
  }

  public String getGeneratedName() {
    return generatedName;
  }

  public void setGeneratedName(String generatedName) {
    this.generatedName = generatedName;
  }

  public MvcRoute addHttpMethod(TypeElement annotation) {
    var annotationMirror =
        this.method.getAnnotationMirrors().stream()
            .filter(it -> it.getAnnotationType().asElement().equals(annotation))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Annotation not found: " + annotation));
    annotationMap.put(annotation, annotationMirror);
    return this;
  }

  public MvcRouter getRouter() {
    return router;
  }

  public List<MvcParameter> getParameters() {
    return parameters;
  }

  public ExecutableElement getMethod() {
    return method;
  }

  public List<String> getRawParameterTypes() {
    return parameters.stream()
        .map(MvcParameter::getType)
        .map(TypeDefinition::getRawType)
        .map(TypeMirror::toString)
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
    buffer.append(method.getSimpleName()).append("()").append(" {}");
    return buffer.toString();
  }

  private Optional<String> dispatch() {
    var dispatch = dispatch(method);
    return dispatch.isEmpty() ? dispatch(router.getTargetType()) : dispatch;
  }

  private Optional<String> dispatch(Element element) {
    return Optional.ofNullable(findAnnotationByName(element, "io.jooby.annotation.Dispatch"))
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
            .map(type -> CodeBlock.of("io.jooby.MediaType.valueOf($S)", type).toString())
            .collect(Collectors.joining(", ", "java.util.List.of(", ")")));
  }

  /**
   * Kotlin suspend function has a <code>kotlin.coroutines.Continuation</code> as last parameter.
   *
   * @return True for Kotlin suspend function.
   */
  private boolean isSuspendFun() {
    var parameters = getRawParameterTypes();
    return !parameters.isEmpty()
        && getRawParameterTypes()
            .get(parameters.size() - 1)
            .equals("kotlin.coroutines.Continuation");
  }

  private CodeBlock javadocComment(List<String> parameters) {
    return CodeBlock.of(
        "/** See {@link $L#$L($L) */\n",
        router.getTargetType().getSimpleName(),
        getMethodName(),
        String.join(", ", parameters));
  }
}
