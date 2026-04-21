/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt.ws;

import io.jooby.internal.apt.*;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.Map;
import java.util.StringJoiner;

import static io.jooby.internal.apt.CodeBlock.*;
import static java.lang.System.lineSeparator;

public class WsRoute extends WebRoute<WsRouter> {

  private static final Map<String, WsLifecycle> LIFECYCLE_ANNOTATIONS =
      Map.of(
          "io.jooby.annotation.ws.OnConnect", WsLifecycle.CONNECT,
          "io.jooby.annotation.ws.OnMessage", WsLifecycle.MESSAGE,
          "io.jooby.annotation.ws.OnClose", WsLifecycle.CLOSE,
          "io.jooby.annotation.ws.OnError", WsLifecycle.ERROR);

  private WsLifecycle wsLifecycle;

  public WsRoute(WsRouter router, ExecutableElement method) {
    super(router, method);
    chekWsAnnotations();
  }

  private void chekWsAnnotations() {
    for (var entry : LIFECYCLE_ANNOTATIONS.entrySet()) {
      if (AnnotationSupport.findAnnotationByName(this.method, entry.getKey()) != null) {
        this.wsLifecycle = entry.getValue();
        validateLifecycleParameters(context, method);
        break;
      }
    }
  }

  public WsLifecycle getWsLifecycle() {
    return wsLifecycle;
  }

  @Override
  public boolean hasBeanValidation() {
    return false;
  }

  @Override
  public TypeDefinition getReturnType() {
    var types = context.getProcessingEnvironment().getTypeUtils();
    return new TypeDefinition(types, method.getReturnType());
  }

  public void appendBody(boolean kt, StringBuilder buffer, String indent) {
    buffer.append(statement(indent, var(kt), "c = this.factory.apply(ctx)", semicolon(kt)));

    TypeDefinition wsReturnType = getReturnType();
    var expr = invocation(kt);

    if (isUncheckedCast()) {
      buffer
          .append(indent)
          .append(
              kt ? "@Suppress(\"UNCHECKED_CAST\") " : "@SuppressWarnings(\"unchecked\") ")
          .append(lineSeparator());
    }

    if (wsReturnType.isVoid()) {
      buffer.append(statement(indent, expr, semicolon(kt)));
      return;
    }

    buffer.append(
        statement(
            indent, kt ? "val" : "var", " __wsReturn = ", expr, semicolon(kt)));
    String rawErasure = wsReturnType.getRawType().toString();
    switch (rawErasure) {
      case "java.lang.String", "byte[]", "java.nio.ByteBuffer" ->
          buffer.append(statement(indent, "ws.send(__wsReturn)", semicolon(kt)));
      default -> buffer.append(statement(indent, "ws.render(__wsReturn)", semicolon(kt)));
    }
  }

  public String invocation(boolean kt) {
    return makeCall(kt, paramList(kt), false, false);
  }

  private String paramList(boolean kt) {
    var joiner = new StringJoiner(", ", "(", ")");
    for (var param : getParameters(true)) {
      joiner.add(websocketArgumentExpression(param, kt));
    }
    return joiner.toString();
  }

  private String websocketArgumentExpression(MvcParameter parameter, boolean kt) {
    String rawParamType = parameter.getType().getRawType().toString();
    if (wsLifecycle == WsLifecycle.MESSAGE) {
      if (WsParamTypes.getAllowedTypes(WsLifecycle.MESSAGE).contains(rawParamType)) {
        return WsParamTypes.generateArgumentName(rawParamType);
      }

      var mvcBodyExpr =
          ParameterGenerator.BodyParam.toSourceCode(
              kt,
              this,
              null,
              parameter.getType(),
              parameter.variableElement(),
              parameter.getName(),
              parameter.isNullable(kt));
      return mvcExprToWsExpr(mvcBodyExpr);
    }

    String expr = WsParamTypes.generateArgumentName(rawParamType);
    if (expr != null) {
      return expr;
    }

    getContext()
        .error("Unsupported websocket handler parameter type: %s.", rawParamType);
    return "null";
  }

  private static String mvcExprToWsExpr(String mvcBodyExpr) {
    String s = mvcBodyExpr;
    s = s.replace("ctx.body().", "message.");
    s = s.replace("ctx.body(", "message.to(");
    return s;
  }

  private void validateLifecycleParameters(MvcContext context, ExecutableElement method) {
    var env = context.getProcessingEnvironment();
    var types = env.getTypeUtils();
    var throwableType = env.getElementUtils().getTypeElement(Throwable.class.getName()).asType();
    var allowed = WsParamTypes.getAllowedTypes(wsLifecycle);

    for (VariableElement parameter : method.getParameters()) {
      TypeMirror rawMirror = websocketParameterRawType(types, parameter);
      var raw = rawMirror.toString();
      if (allowed.contains(raw)) {
        continue;
      }

      if (wsLifecycle == WsLifecycle.ERROR
          && throwableType != null
          && types.isAssignable(rawMirror, throwableType)) {
        continue;
      }

      if (wsLifecycle == WsLifecycle.MESSAGE) {
        continue;
      }

      context.error(
          "Illegal parameter type %s on websocket %s method %s#%s",
          raw,
          wsLifecycle.name().toLowerCase(),
          ((TypeElement) method.getEnclosingElement()).getQualifiedName(),
          method.getSimpleName());
    }
  }

  private static TypeMirror websocketParameterRawType(javax.lang.model.util.Types types,
                                                      VariableElement parameter) {
    return new TypeDefinition(types, parameter.asType()).getRawType();
  }
}
