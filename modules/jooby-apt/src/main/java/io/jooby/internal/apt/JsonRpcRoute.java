/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt;

import static io.jooby.internal.apt.AnnotationSupport.VALUE;
import static io.jooby.internal.apt.CodeBlock.*;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Consumer;

import javax.lang.model.element.ExecutableElement;

public class JsonRpcRoute extends WebRoute {

  public JsonRpcRoute(WebRouter<?> router, ExecutableElement method) {
    super(router, method);
  }

  public String getJsonRpcMethodName() {
    var annotation = AnnotationSupport.findAnnotationByName(method, "io.jooby.annotation.JsonRpc");
    if (annotation != null) {
      var val =
          AnnotationSupport.findAnnotationValue(annotation, VALUE).stream().findFirst().orElse("");
      if (!val.isEmpty()) return val;
    }
    return getMethodName();
  }

  public List<String> generateJsonRpcDispatchCase(boolean kt) {
    var buffer = new ArrayList<String>();
    var paramList = new StringJoiner(", ", "(", ")");

    // Check if we have any parameters that actually need to be parsed from the JSON payload
    boolean needsReader =
        parameters.stream()
            .anyMatch(
                p -> {
                  String type = p.getType().toString();
                  return !type.equals("io.jooby.Context")
                      && !type.startsWith("kotlin.coroutines.Continuation");
                });

    if (needsReader) {
      if (kt) {
        buffer.add(statement(indent(8), "parser.reader(req.params).use { reader ->"));
      } else {
        buffer.add(statement(indent(8), "try (var reader = parser.reader(req.getParams())) {"));
      }
    }

    buffer.addAll(generateRpcParameter(kt, paramList::add));

    int callIndent = needsReader ? 10 : 8;
    var call = CodeBlock.of("c.", getMethodName(), paramList.toString());

    if (returnType.isVoid()) {
      buffer.add(statement(indent(callIndent), call, semicolon(kt)));
      buffer.add(statement(indent(callIndent), kt ? "null" : "return null", semicolon(kt)));
    } else {
      buffer.add(statement(indent(callIndent), kt ? call : "return " + call, semicolon(kt)));
    }

    if (needsReader) {
      buffer.add(statement(indent(8), "}"));
    }

    return buffer;
  }

  private List<String> generateRpcParameter(boolean kt, Consumer<String> arguments) {
    var statements = new ArrayList<String>();
    var decoderInterface = "io.jooby.rpc.jsonrpc.JsonRpcDecoder";
    int baseIndent = 10;

    for (var parameter : parameters) {
      var paramenterName = parameter.getName();
      var type = type(kt, parameter.getType().toString());
      boolean isNullable = parameter.isNullable(kt);

      switch (parameter.getType().getRawType().toString()) {
        case "io.jooby.Context":
          arguments.accept("ctx");
          break;
        case "int",
        "long",
        "double",
        "boolean",
        "java.lang.String",
        "java.lang.Integer",
        "java.lang.Long",
        "java.lang.Double",
        "java.lang.Boolean":
          var simpleType = type.startsWith("java.lang.") ? type.substring(10) : type;
          if (simpleType.equals("Integer") || simpleType.equals("int")) simpleType = "Int";
          var readName =
              "next" + Character.toUpperCase(simpleType.charAt(0)) + simpleType.substring(1);

          if (isNullable) {
            if (kt) {
              statements.add(
                  statement(
                      indent(baseIndent),
                      "val ",
                      paramenterName,
                      " = if (reader.nextIsNull(",
                      string(paramenterName),
                      ")) null else reader.",
                      readName,
                      "(",
                      string(paramenterName),
                      ")"));
            } else {
              statements.add(
                  statement(
                      indent(baseIndent),
                      var(kt),
                      paramenterName,
                      " = reader.nextIsNull(",
                      string(paramenterName),
                      ") ? null : reader.",
                      readName,
                      "(",
                      string(paramenterName),
                      ")",
                      semicolon(kt)));
            }
          } else {
            statements.add(
                statement(
                    indent(baseIndent),
                    var(kt),
                    paramenterName,
                    " = reader.",
                    readName,
                    "(",
                    string(paramenterName),
                    ")",
                    semicolon(kt)));
          }
          arguments.accept(paramenterName);
          break;
        default:
          if (kt) {
            statements.add(
                statement(
                    indent(baseIndent),
                    "val ",
                    paramenterName,
                    "Decoder: ",
                    decoderInterface,
                    "<",
                    type,
                    "> = parser.decoder(",
                    parameter.getType().toSourceCode(kt),
                    ")",
                    semicolon(kt)));
            if (isNullable) {
              statements.add(
                  statement(
                      indent(baseIndent),
                      "val ",
                      paramenterName,
                      " = if (reader.nextIsNull(",
                      string(paramenterName),
                      ")) null else reader.nextObject(",
                      string(paramenterName),
                      ", ",
                      paramenterName,
                      "Decoder)"));
            } else {
              statements.add(
                  statement(
                      indent(baseIndent),
                      "val ",
                      paramenterName,
                      " = reader.nextObject(",
                      string(paramenterName),
                      ", ",
                      paramenterName,
                      "Decoder)",
                      semicolon(kt)));
            }
          } else {
            statements.add(
                statement(
                    indent(baseIndent),
                    decoderInterface,
                    "<",
                    type,
                    "> ",
                    paramenterName,
                    "Decoder = parser.decoder(",
                    parameter.getType().toSourceCode(kt),
                    ")",
                    semicolon(kt)));
            if (isNullable) {
              statements.add(
                  statement(
                      indent(baseIndent),
                      parameter.getType().toString(),
                      " ",
                      paramenterName,
                      " = reader.nextIsNull(",
                      string(paramenterName),
                      ") ? null : reader.nextObject(",
                      string(paramenterName),
                      ", ",
                      paramenterName,
                      "Decoder)",
                      semicolon(kt)));
            } else {
              statements.add(
                  statement(
                      indent(baseIndent),
                      parameter.getType().toString(),
                      " ",
                      paramenterName,
                      " = reader.nextObject(",
                      string(paramenterName),
                      ", ",
                      paramenterName,
                      "Decoder)",
                      semicolon(kt)));
            }
          }
          arguments.accept(paramenterName);
          break;
      }
    }
    return statements;
  }
}
