/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt;

import static io.jooby.internal.apt.CodeBlock.clazz;
import static io.jooby.internal.apt.CodeBlock.type;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;

public abstract class WebRoute<R extends WebRouter<?>> {
  protected final MvcContext context;
  protected final ExecutableElement method;
  protected final List<MvcParameter> parameters;
  protected final TypeDefinition returnType;
  protected final boolean suspendFun;
  protected final boolean hasBeanValidation;
  protected final R router;
  private boolean uncheckedCast;

  public WebRoute(R router, ExecutableElement method) {
    this.context = router.context;
    this.router = router;
    this.method = method;
    this.parameters =
        method.getParameters().stream().map(it -> new MvcParameter(context, this, it)).toList();
    this.hasBeanValidation = parameters.stream().anyMatch(MvcParameter::isRequireBeanValidation);
    this.suspendFun =
        !parameters.isEmpty()
            && parameters.getLast().getType().is("kotlin.coroutines.Continuation");
    this.returnType =
        new TypeDefinition(
            context.getProcessingEnvironment().getTypeUtils(), method.getReturnType());
  }

  public R getRouter() {
    return router;
  }

  public MvcContext getContext() {
    return context;
  }

  public ExecutableElement getMethod() {
    return method;
  }

  public String getMethodName() {
    return method.getSimpleName().toString();
  }

  public boolean isSuspendFun() {
    return suspendFun;
  }

  public boolean hasBeanValidation() {
    return hasBeanValidation;
  }

  public List<MvcParameter> getParameters(boolean skipCoroutine) {
    return parameters.stream()
        .filter(type -> !skipCoroutine || !type.getType().is("kotlin.coroutines.Continuation"))
        .toList();
  }

  static String leadingSlash(String path) {
    if (path == null || path.isEmpty() || path.equals("/")) {
      return "/";
    }
    return path.charAt(0) == '/' ? path : "/" + path;
  }

  public TypeDefinition getReturnType() {
    var processingEnv = context.getProcessingEnvironment();
    var types = processingEnv.getTypeUtils();
    var elements = processingEnv.getElementUtils();

    if (returnType.isVoid()) {
      return new TypeDefinition(types, elements.getTypeElement("io.jooby.StatusCode").asType());
    } else if (isSuspendFun()) {
      var continuation = parameters.getLast().getType();
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

  public List<String> getRawParameterTypes(boolean skipCoroutine, boolean kt) {
    return getParameters(skipCoroutine).stream()
        .map(MvcParameter::getType)
        .map(TypeDefinition::getRawType)
        .map(TypeMirror::toString)
        .map(it -> type(kt, it))
        .toList();
  }

  public List<String> getRawParameterTypes(
      boolean skipCoroutine, boolean kt, boolean keepJavaLang) {
    return getParameters(skipCoroutine).stream()
        .map(MvcParameter::getType)
        .map(TypeDefinition::getRawType)
        .map(TypeMirror::toString)
        .map(it -> keepJavaLang ? it : type(kt, it))
        .toList();
  }

  /**
   * Returns the return type of the route method. Used to determine if the route returns a reactive
   * type that requires static imports.
   *
   * @return The return type of the route handler.
   */
  public TypeMirror getReturnTypeHandler() {
    return getReturnType().getRawType();
  }

  public boolean isUncheckedCast() {
    return uncheckedCast;
  }

  public void setUncheckedCast(boolean value) {
    this.uncheckedCast = value;
  }

  protected List<String> getJavaMethodSignature(boolean kt) {
    return getParameters(false).stream()
        .map(
            it -> {
              var type = it.getType();
              if (kt && type.isPrimitive()) {
                return type(kt, type.getRawType().toString());
              }
              return type.getRawType().toString();
            })
        .map(it -> it + clazz(kt))
        .toList();
  }

  protected boolean isNullableKotlinReturn() {
    // SpotBugs/FindBugs vs JSpecify:
    return Stream.of(method.getAnnotationMirrors(), method.getReturnType().getAnnotationMirrors())
        .flatMap(List::stream)
        .map(javax.lang.model.element.AnnotationMirror::getAnnotationType)
        .map(java.util.Objects::toString)
        .anyMatch(AnnotationSupport.NULLABLE);
  }

  protected String makeCall(
      boolean kt, String paramList, boolean preventCast, boolean isRpcWrapper) {
    var customReturnType = getReturnType();
    var castStr =
        preventCast ? "" : customReturnType.getArgumentsString(kt, false, Set.of(TypeKind.TYPEVAR));

    // Force cast ONLY if there's a Type Variable (<E> -> <Any>)
    // OR if it's an RPC wrapper where strict Java generics conflict with Kotlin's nullable generics
    var needsCast =
        !castStr.isEmpty()
            || (kt && !preventCast && isRpcWrapper && !customReturnType.getArguments().isEmpty());

    var kotlinNotEnoughTypeInformation = !castStr.isEmpty() && kt ? "<Any>" : "";

    var call = "c." + this.method.getSimpleName() + kotlinNotEnoughTypeInformation + paramList;

    if (needsCast) {
      setUncheckedCast(true);
      var returnTypeString = CodeBlock.type(kt, customReturnType.toString());
      call = kt ? call + " as " + returnTypeString : "(" + returnTypeString + ") " + call;
    }
    return call;
  }

  protected String box(String type) {
    return switch (type) {
      case "int" -> "Integer";
      case "long" -> "Long";
      case "double" -> "Double";
      case "float" -> "Float";
      case "boolean" -> "Boolean";
      case "byte" -> "Byte";
      case "short" -> "Short";
      case "char" -> "Character";
      default -> type;
    };
  }
}
