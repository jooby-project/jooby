/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt;

import static io.jooby.internal.apt.AnnotationSupport.VALUE;
import static io.jooby.internal.apt.CodeBlock.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;

public class TrpcRoute extends WebRoute {
  private final HttpMethod resolvedTrpcMethod;
  private String generatedName;

  public TrpcRoute(WebRouter<?> router, ExecutableElement method) {
    super(router, method);
    this.resolvedTrpcMethod = discoverTrpcMethod();
    this.generatedName = method.getSimpleName().toString();
  }

  public void setGeneratedName(String generatedName) {
    this.generatedName = generatedName;
  }

  public String getGeneratedName() {
    return generatedName;
  }

  private HttpMethod discoverTrpcMethod() {
    if (AnnotationSupport.findAnnotationByName(method, "io.jooby.annotation.Trpc.Query") != null)
      return HttpMethod.GET;
    if (AnnotationSupport.findAnnotationByName(method, "io.jooby.annotation.Trpc.Mutation") != null)
      return HttpMethod.POST;
    if (AnnotationSupport.findAnnotationByName(method, "io.jooby.annotation.Trpc") != null) {
      if (HttpMethod.GET.matches(method)) return HttpMethod.GET;
      return HttpMethod.POST; // Default fallback for @Trpc missing explicit Query/Mutation mapping
    }
    return null;
  }

  public String trpcPath() {
    var namespace =
        Optional.ofNullable(
                AnnotationSupport.findAnnotationByName(
                    method.getEnclosingElement(), "io.jooby.annotation.Trpc"))
            .flatMap(it -> AnnotationSupport.findAnnotationValue(it, VALUE).stream().findFirst())
            .map(it -> it + ".")
            .orElse("");

    var procedure =
        Stream.of(
                "io.jooby.annotation.Trpc.Query",
                "io.jooby.annotation.Trpc.Mutation",
                "io.jooby.annotation.Trpc")
            .map(it -> AnnotationSupport.findAnnotationByName(method, it))
            .filter(Objects::nonNull)
            .findFirst()
            .flatMap(it -> AnnotationSupport.findAnnotationValue(it, VALUE).stream().findFirst())
            .orElse(method.getSimpleName().toString());

    return Stream.of("trpc", namespace + procedure)
        .map(segment -> segment.startsWith("/") ? segment.substring(1) : segment)
        .collect(Collectors.joining("/", "/", ""));
  }

  public List<String> generateMapping(boolean kt, String routerName) {
    List<String> block = new ArrayList<>();
    var targetMethod =
        "trpc" + generatedName.substring(0, 1).toUpperCase() + generatedName.substring(1);
    var dslMethod = resolvedTrpcMethod.name().toLowerCase();
    var path = trpcPath();

    var thisRef =
        isSuspendFun() ? "this@" + context.generateRouterName(routerName) + "::" : "this::";

    block.add(
        CodeBlock.statement(
            kt
                ? "/** See [" + routerName + "." + getMethodName() + "] */"
                : "/** See {@link " + routerName + "#" + getMethodName() + "} */"));
    block.add(
        of(
            isSuspendFun() ? "" : "app.",
            dslMethod,
            "(",
            string(path.startsWith("/") ? path : "/" + path),
            ", ",
            context.pipeline(
                getReturnType().getRawType(), methodReference(kt, thisRef, targetMethod))));

    if (context.nonBlocking(getReturnType().getRawType()) || isSuspendFun())
      block.add(statement(indent(2), ".setNonBlocking(true)"));

    var lastStatement = block.get(block.size() - 1);
    block.set(
        block.size() - 1,
        lastStatement + semicolon(kt) + System.lineSeparator() + System.lineSeparator());

    return block;
  }

  private String methodReference(boolean kt, String thisRef, String methodName) {
    if (kt) {
      var generics = returnType.getArgumentsString(kt, true, Set.of(TypeKind.TYPEVAR));
      if (!generics.isEmpty()) return CodeBlock.of(") { ctx -> ", methodName, generics, "(ctx) }");
    }
    return thisRef + methodName + ")";
  }

  public List<String> generateHandlerCall(boolean kt) {
    var buffer = new ArrayList<String>();
    var methodName =
        "trpc" + generatedName.substring(0, 1).toUpperCase() + generatedName.substring(1);
    var paramList = new StringJoiner(", ", "(", ")");

    var returnTypeGenerics =
        getReturnType().getArgumentsString(kt, false, Set.of(TypeKind.TYPEVAR));
    var returnTypeString = type(kt, getReturnType().toString());

    var reactive = context.getReactiveType(returnType.getRawType());
    var isReactiveVoid = false;
    var innerReactiveType = "Object";
    var methodReturnTypeString = returnTypeString;

    if (reactive != null) {
      var rawReactiveType = type(kt, returnType.getRawType().toString());
      if (!returnType.getArguments().isEmpty()) {
        innerReactiveType = type(kt, returnType.getArguments().get(0).getRawType().toString());
        if (innerReactiveType.equals("java.lang.Void") || innerReactiveType.equals("Void")) {
          isReactiveVoid = true;
          innerReactiveType = kt ? "Unit" : "Void";
        }
      } else if (rawReactiveType.contains("Completable")) {
        isReactiveVoid = true;
        innerReactiveType = kt ? "Unit" : "Void";
      }
      methodReturnTypeString =
          rawReactiveType + "<io.jooby.rpc.trpc.TrpcResponse<" + innerReactiveType + ">>";
    } else {
      methodReturnTypeString =
          "io.jooby.rpc.trpc.TrpcResponse<"
              + (returnType.isVoid() ? (kt ? "Unit" : "Void") : returnTypeString)
              + ">";
    }

    if (kt) {
      buffer.add(
          statement(
              "fun ",
              returnTypeGenerics,
              methodName,
              "(ctx: io.jooby.Context): ",
              methodReturnTypeString,
              " {"));
    } else {
      buffer.add(
          statement(
              "public ",
              returnTypeGenerics,
              methodReturnTypeString,
              " ",
              methodName,
              "(io.jooby.Context ctx) throws Exception {"));
    }

    int controllerIndent = parameters.isEmpty() ? 2 : 4;

    if (!parameters.isEmpty()) {
      buffer.add(
          statement(
              indent(2),
              var(kt),
              "parser = ctx.require(io.jooby.rpc.trpc.TrpcParser",
              clazz(kt),
              ")",
              semicolon(kt)));
      long payloadCount =
          parameters.stream()
              .filter(
                  p -> {
                    String t = p.getType().getRawType().toString();
                    return !t.equals("io.jooby.Context")
                        && !p.getType().is("kotlin.coroutines.Continuation");
                  })
              .count();
      boolean isTuple = payloadCount > 1;

      if (resolvedTrpcMethod == HttpMethod.GET) {
        buffer.add(
            statement(
                indent(2),
                var(kt),
                "input = ctx.query(",
                string("input"),
                ").value()",
                semicolon(kt)));
        if (isTuple) {
          if (kt)
            buffer.add(
                statement(
                    indent(2),
                    "if (input?.trim()?.let { it.startsWith('[') && it.endsWith(']') } != true)"
                        + " throw IllegalArgumentException(",
                    string("tRPC input for multiple arguments must be a JSON array (tuple)"),
                    ")"));
          else
            buffer.add(
                statement(
                    indent(2),
                    "if (input == null || input.length() < 2 || input.charAt(0) != '[' ||"
                        + " input.charAt(input.length() - 1) != ']') throw new"
                        + " IllegalArgumentException(",
                    string("tRPC input for multiple arguments must be a JSON array (tuple)"),
                    ");"));
        }
      } else {
        buffer.add(statement(indent(2), var(kt), "input = ctx.body().bytes()", semicolon(kt)));
        if (isTuple) {
          if (kt)
            buffer.add(
                statement(
                    indent(2),
                    "if (input.size < 2 || input[0] != '['.code.toByte() || input[input.size - 1]"
                        + " != ']'.code.toByte()) throw IllegalArgumentException(",
                    string("tRPC body for multiple arguments must be a JSON array (tuple)"),
                    ")"));
          else
            buffer.add(
                statement(
                    indent(2),
                    "if (input.length < 2 || input[0] != '[' || input[input.length - 1] != ']')"
                        + " throw new IllegalArgumentException(",
                    string("tRPC body for multiple arguments must be a JSON array (tuple)"),
                    ");"));
        }
      }

      if (kt) {
        buffer.add(
            statement(
                indent(2), "parser.reader(input, ", String.valueOf(isTuple), ").use { reader -> "));
      } else {
        buffer.add(
            statement(
                indent(2),
                "try (var reader = parser.reader(input, ",
                String.valueOf(isTuple),
                ")) {"));
      }

      // Read parameters optimally
      for (var parameter : parameters) {
        var paramenterName = parameter.getName();
        var rawType = parameter.getType().getRawType().toString();
        var type = type(kt, parameter.getType().toString());
        boolean isNullable = parameter.isNullable(kt);

        if (rawType.equals("io.jooby.Context")) {
          paramList.add("ctx");
          continue;
        }
        if (rawType.equals("kotlin.coroutines.Continuation")) {
          continue;
        }

        switch (rawType) {
          case "int":
          case "long":
          case "double":
          case "boolean":
          case "java.lang.String":
          case "java.lang.Integer":
          case "java.lang.Long":
          case "java.lang.Double":
          case "java.lang.Boolean":
            var simpleType = type.startsWith("java.lang.") ? type.substring(10) : type;
            if (simpleType.equals("Integer") || simpleType.equals("int")) simpleType = "Int";
            var readName =
                "next" + Character.toUpperCase(simpleType.charAt(0)) + simpleType.substring(1);

            if (isNullable) {
              if (kt) {
                buffer.add(
                    statement(
                        indent(4),
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
                buffer.add(
                    statement(
                        indent(4),
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
              buffer.add(
                  statement(
                      indent(4),
                      var(kt),
                      paramenterName,
                      " = reader.",
                      readName,
                      "(",
                      string(paramenterName),
                      ")",
                      semicolon(kt)));
            }
            paramList.add(paramenterName);
            break;

          case "byte":
          case "short":
          case "float":
          case "char":
          case "java.lang.Byte":
          case "java.lang.Short":
          case "java.lang.Float":
          case "java.lang.Character":
            var isChar = type.equals("char") || type.equals("java.lang.Character");
            var isFloat = type.equals("float") || type.equals("java.lang.Float");
            var readMethod = isFloat ? "nextDouble" : (isChar ? "nextString" : "nextInt");

            if (isNullable) {
              if (kt) {
                var ktCast =
                    isChar
                        ? "?.get(0)"
                        : "?.to"
                            + Character.toUpperCase(type.replace("java.lang.", "").charAt(0))
                            + type.replace("java.lang.", "").substring(1)
                            + "()";
                buffer.add(
                    statement(
                        indent(4),
                        "val ",
                        paramenterName,
                        " = if (reader.nextIsNull(",
                        string(paramenterName),
                        ")) null else reader.",
                        readMethod,
                        "(",
                        string(paramenterName),
                        ")",
                        ktCast));
              } else {
                var targetType = type.replace("java.lang.", "");
                var javaPrefix = isChar ? "" : "(" + targetType + ") ";
                var javaSuffix = isChar ? ".charAt(0)" : "";
                buffer.add(
                    statement(
                        indent(4),
                        var(kt),
                        paramenterName,
                        " = reader.nextIsNull(",
                        string(paramenterName),
                        ") ? null : ",
                        javaPrefix,
                        "reader.",
                        readMethod,
                        "(",
                        string(paramenterName),
                        ")",
                        javaSuffix,
                        semicolon(kt)));
              }
            } else {
              if (kt) {
                var ktCast =
                    isChar
                        ? "[0]"
                        : ".to"
                            + Character.toUpperCase(type.replace("java.lang.", "").charAt(0))
                            + type.replace("java.lang.", "").substring(1)
                            + "()";
                buffer.add(
                    statement(
                        indent(4),
                        var(kt),
                        paramenterName,
                        " = reader.",
                        readMethod,
                        "(",
                        string(paramenterName),
                        ")",
                        ktCast,
                        semicolon(kt)));
              } else {
                var targetType = type.replace("java.lang.", "");
                var javaPrefix = isChar ? "" : "(" + targetType + ") ";
                var javaSuffix = isChar ? ".charAt(0)" : "";
                buffer.add(
                    statement(
                        indent(4),
                        var(kt),
                        paramenterName,
                        " = ",
                        javaPrefix,
                        "reader.",
                        readMethod,
                        "(",
                        string(paramenterName),
                        ")",
                        javaSuffix,
                        semicolon(kt)));
              }
            }
            paramList.add(paramenterName);
            break;

          default:
            var genericType = kt ? type : box(type); // Box primitives for Java Generics
            if (kt) {
              buffer.add(
                  statement(
                      indent(4),
                      "val ",
                      paramenterName,
                      "Decoder: io.jooby.rpc.trpc.TrpcDecoder<",
                      type,
                      "> = parser.decoder(",
                      parameter.getType().toSourceCode(kt),
                      ")"));
              if (isNullable) {
                buffer.add(
                    statement(
                        indent(4),
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
                buffer.add(
                    statement(
                        indent(4),
                        "val ",
                        paramenterName,
                        " = reader.nextObject(",
                        string(paramenterName),
                        ", ",
                        paramenterName,
                        "Decoder)"));
              }
            } else {
              buffer.add(
                  statement(
                      indent(4),
                      "io.jooby.rpc.trpc.TrpcDecoder<",
                      genericType,
                      "> ",
                      paramenterName,
                      "Decoder = parser.decoder(",
                      parameter.getType().toSourceCode(kt),
                      ")",
                      semicolon(false)));
              if (isNullable) {
                buffer.add(
                    statement(
                        indent(4),
                        type,
                        " ",
                        paramenterName,
                        " = reader.nextIsNull(",
                        string(paramenterName),
                        ") ? null : reader.nextObject(",
                        string(paramenterName),
                        ", ",
                        paramenterName,
                        "Decoder)",
                        semicolon(false)));
              } else {
                buffer.add(
                    statement(
                        indent(4),
                        type,
                        " ",
                        paramenterName,
                        " = reader.nextObject(",
                        string(paramenterName),
                        ", ",
                        paramenterName,
                        "Decoder)",
                        semicolon(false)));
              }
            }
            paramList.add(paramenterName);
            break;
        }
      }
    }

    buffer.add(
        statement(indent(controllerIndent), var(kt), "c = this.factory.apply(ctx)", semicolon(kt)));

    // Leverage shared WebRoute logic for casting and type erasure!
    // Pass 'true' for isRpcWrapper so it safely casts List<Movie?> to List<Movie>
    String call = buildMethodCall(kt, paramList.toString(), false, true);
    boolean nullable = kt && isNullableKotlinReturn();

    if (reactive != null) {
      if (isReactiveVoid) {
        var handler = reactive.handlerType();
        if (handler.contains("Reactor")) {
          buffer.add(
              statement(
                  indent(controllerIndent),
                  "return ",
                  call,
                  ".then(reactor.core.publisher.Mono.just(io.jooby.rpc.trpc.TrpcResponse.empty()))",
                  semicolon(kt)));
        } else if (handler.contains("Mutiny")) {
          buffer.add(
              statement(
                  indent(controllerIndent),
                  "return ",
                  call,
                  ".replaceWith(io.jooby.rpc.trpc.TrpcResponse.empty())",
                  semicolon(kt)));
        } else if (handler.contains("ReactiveSupport")) {
          buffer.add(
              statement(
                  indent(controllerIndent),
                  "return ",
                  call,
                  ".thenApply(x -> io.jooby.rpc.trpc.TrpcResponse.empty())",
                  semicolon(kt)));
        } else if (handler.contains("Reactivex")) {
          buffer.add(
              statement(
                  indent(controllerIndent),
                  "return ",
                  call,
                  ".toSingleDefault(io.jooby.rpc.trpc.TrpcResponse.empty())",
                  semicolon(kt)));
        } else {
          buffer.add(
              statement(
                  indent(controllerIndent),
                  "return ",
                  call,
                  ".map(x -> io.jooby.rpc.trpc.TrpcResponse.empty())",
                  semicolon(kt)));
        }
      } else {
        var handler = reactive.handlerType();
        if (kt) {
          buffer.add(
              statement(
                  indent(controllerIndent),
                  "return ",
                  call,
                  ".map { io.jooby.rpc.trpc.TrpcResponse.of(it) }"));
        } else {
          if (handler.contains("ReactiveSupport")) {
            buffer.add(
                statement(
                    indent(controllerIndent),
                    "return ",
                    call,
                    ".thenApply(io.jooby.rpc.trpc.TrpcResponse::of)",
                    semicolon(kt)));
          } else if (handler.contains("Mutiny")) {
            buffer.add(
                statement(
                    indent(controllerIndent),
                    "return ",
                    call,
                    ".onItem().transform(io.jooby.rpc.trpc.TrpcResponse::of)",
                    semicolon(kt)));
          } else {
            // Reactor (Mono), RxJava (Single), etc.
            buffer.add(
                statement(
                    indent(controllerIndent),
                    "return ",
                    call,
                    ".map(io.jooby.rpc.trpc.TrpcResponse::of)",
                    semicolon(kt)));
          }
        }
      }
    } else if (returnType.isVoid()) {
      buffer.add(statement(indent(controllerIndent), call, semicolon(kt)));
      buffer.add(
          statement(
              indent(controllerIndent),
              "return io.jooby.rpc.trpc.TrpcResponse.empty()",
              semicolon(kt)));
    } else {
      buffer.add(
          statement(
              indent(controllerIndent),
              "return io.jooby.rpc.trpc.TrpcResponse.of(",
              call,
              nullable ? "!!" : "", // Shared nullability check
              ")",
              semicolon(kt)));
    }

    if (!parameters.isEmpty()) buffer.add(statement(indent(2), "}"));
    buffer.add(statement("}", System.lineSeparator()));

    // Suppress both UNCHECKED_CAST and USELESS_CAST to keep the Kotlin compiler perfectly quiet
    if (isUncheckedCast()) {
      if (kt) buffer.addFirst(statement("@Suppress(\"UNCHECKED_CAST\", \"USELESS_CAST\")"));
      else buffer.addFirst(statement("@SuppressWarnings(\"unchecked\")"));
    }

    return buffer;
  }
}
