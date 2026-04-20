/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt.ws;

import static io.jooby.internal.apt.CodeBlock.semicolon;
import static io.jooby.internal.apt.CodeBlock.statement;
import static java.lang.System.lineSeparator;

import java.util.StringJoiner;

import javax.lang.model.element.ExecutableElement;

import io.jooby.internal.apt.CodeBlock;
import io.jooby.internal.apt.MvcParameter;
import io.jooby.internal.apt.TypeDefinition;
import io.jooby.internal.apt.WebRoute;

public class WsHandlerMethod extends WebRoute<WsRouter> {

  public WsHandlerMethod(WsRouter router, ExecutableElement method) {
    super(router, method);
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
    buffer.append(
        statement(indent, CodeBlock.var(kt), "c = this.factory.apply(ctx)", semicolon(kt)));

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
      default ->
          buffer.append(statement(indent, "ws.render(__wsReturn)", semicolon(kt)));
    }
  }

  public String invocation(boolean kt) {
    return makeCall(kt, paramList(), false, false);
  }

  private String paramList() {
    var joiner = new StringJoiner(", ", "(", ")");
    for (var param : getParameters(true)) {
      joiner.add(wsParameterName(param));
    }
    return joiner.toString();
  }

  private String wsParameterName(MvcParameter parameter) {
    String rawParamType = parameter.getType().getRawType().toString();
    var name = WsParamTypes.generateArgumentName(rawParamType);
    if (name != null) {
      return name;
    }

    getContext()
        .error("Unsupported websocket handler parameter type: %s.", rawParamType);
    return "null";
  }
}
