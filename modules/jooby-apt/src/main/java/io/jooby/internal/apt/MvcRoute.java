/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt;

import static io.jooby.internal.apt.AnnotationSupport.*;
import static io.jooby.internal.apt.CodeBlock.*;
import static java.lang.System.*;
import static java.util.Optional.ofNullable;

import java.util.*;
import java.util.function.Consumer;
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
  private final Set<String> pending = new HashSet<>();
  private boolean isTrpc = false;
  private HttpMethod resolvedTrpcMethod = null;

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

  public String getProjection() {
    var project = AnnotationSupport.findAnnotationByName(method, Types.PROJECT);
    if (project != null) {
      return AnnotationSupport.findAnnotationValue(project, VALUE).stream()
          .findFirst()
          .orElse(null);
    }
    var httpMethod = annotationMap.values().iterator().next();
    var projection = AnnotationSupport.findAnnotationValue(httpMethod, "projection"::equals);
    return projection.stream().findFirst().orElse(null);
  }

  public boolean isProjection() {
    if (returnType.is(Types.PROJECTED)) {
      return false;
    }
    var isProjection = AnnotationSupport.findAnnotationByName(method, Types.PROJECT) != null;
    if (isProjection) {
      return true;
    }
    var httpMethod = annotationMap.values().iterator().next();
    var projection = AnnotationSupport.findAnnotationValue(httpMethod, "projection"::equals);
    return !projection.isEmpty();
  }

  public TypeDefinition getReturnType() {
    var processingEnv = context.getProcessingEnvironment();
    var types = processingEnv.getTypeUtils();
    var elements = processingEnv.getElementUtils();
    if (isProjection()) {
      return new TypeDefinition(types, elements.getTypeElement(Types.PROJECTED).asType(), true);
    } else if (returnType.isVoid()) {
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
      var dslMethod = annotation.getSimpleName().toString().toLowerCase();
      var paths = context.path(router.getTargetType(), method, annotation);
      var targetMethod = methodName;

      if (httpMethod == HttpMethod.tRPC) {
        resolvedTrpcMethod = trpcMethod(method);
        if (resolvedTrpcMethod == null) {
          throw new IllegalArgumentException(
              "tRPC method not found: "
                  + method.getSimpleName()
                  + "() in "
                  + router.getTargetType());
        }
        dslMethod = resolvedTrpcMethod.name().toLowerCase();
        paths = List.of(trpcPath(method));
        targetMethod =
            "trpc" + targetMethod.substring(0, 1).toUpperCase() + targetMethod.substring(1);
        this.isTrpc = true;
      }

      pending.add(targetMethod);

      for (var path : paths) {
        var lastLine = lastHttpMethod && paths.getLast().equals(path);
        block.add(javadocLink);
        block.add(
            statement(
                isSuspendFun() ? "" : "app.",
                dslMethod,
                "(",
                string(leadingSlash(path)),
                ", ",
                context.pipeline(
                    getReturnTypeHandler(), methodReference(kt, thisRef, targetMethod))));
        if (context.nonBlocking(getReturnTypeHandler()) || isSuspendFun()) {
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

  static String leadingSlash(String path) {
    if (path == null || path.isEmpty() || path.equals("/")) {
      return "/";
    }
    return path.charAt(0) == '/' ? path : "/" + path;
  }

  public List<String> generateHandlerCall(boolean kt) {
    var buffer = new ArrayList<String>();
    var methodName =
        isTrpc
            ? "trpc"
                + getGeneratedName().substring(0, 1).toUpperCase()
                + getGeneratedName().substring(1)
            : getGeneratedName();

    if (pending.contains(methodName)) {
      var paramList = new StringJoiner(", ", "(", ")");
      var returnTypeGenerics =
          getReturnType().getArgumentsString(kt, false, Set.of(TypeKind.TYPEVAR));
      var returnTypeString = type(kt, getReturnType().toString());
      var customReturnType = getReturnType();

      if (customReturnType.isProjection()) {
        returnTypeGenerics = "";
        returnTypeString = Types.PROJECTED + "<" + returnType + ">";
      }

      var reactive = isTrpc ? context.getReactiveType(returnType.getRawType()) : null;
      var isReactiveVoid = false;
      var innerReactiveType = "Object";

      // 1. Resolve Target Signature
      var methodReturnTypeString = returnTypeString;
      if (isTrpc) {
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
              rawReactiveType + "<io.jooby.trpc.TrpcResponse<" + innerReactiveType + ">>";
        } else {
          methodReturnTypeString =
              "io.jooby.trpc.TrpcResponse<"
                  + (returnType.isVoid() ? (kt ? "Unit" : "Void") : returnTypeString)
                  + ">";
        }
      }

      var nullable =
          methodCallHeader(
              kt,
              "ctx",
              methodName,
              buffer,
              returnTypeGenerics,
              methodReturnTypeString,
              isTrpc || !method.getThrownTypes().isEmpty());

      int controllerIndent = 2;

      if (isTrpc && !parameters.isEmpty()) {
        controllerIndent = 4;
        buffer.add(
            statement(
                indent(2),
                var(kt),
                "parser = ctx.require(io.jooby.trpc.TrpcParser",
                clazz(kt),
                ")",
                semicolon(kt)));

        // Calculate actual tRPC payload parameters (ignore Context and Coroutines)
        long trpcPayloadCount =
            parameters.stream()
                .filter(
                    p -> {
                      String type = p.getType().getRawType().toString();
                      return !type.equals("io.jooby.Context")
                          && !p.getType().is("kotlin.coroutines.Continuation");
                    })
                .count();
        boolean isTuple = trpcPayloadCount > 1;

        if (resolvedTrpcMethod == HttpMethod.GET) {
          buffer.add(
              statement(
                  indent(2),
                  var(kt),
                  "input = ctx.query(",
                  string("input"),
                  ").value()",
                  semicolon(kt)));

          if (isTuple) { // <-- Use calculated isTuple
            if (kt) {
              buffer.add(
                  statement(
                      indent(2),
                      "if (input?.trim()?.let { it.startsWith('[') && it.endsWith(']') } != true)"
                          + " throw IllegalArgumentException(",
                      string("tRPC input for multiple arguments must be a JSON array (tuple)"),
                      ")"));
            } else {
              buffer.add(
                  statement(
                      indent(2),
                      "if (input == null || input.length() < 2 || input.charAt(0) != '[' ||"
                          + " input.charAt(input.length() - 1) != ']') throw new"
                          + " IllegalArgumentException(",
                      string("tRPC input for multiple arguments must be a JSON array (tuple)"),
                      ");"));
            }
          }
        } else {
          buffer.add(statement(indent(2), var(kt), "input = ctx.body().bytes()", semicolon(kt)));

          if (isTuple) { // <-- Use calculated isTuple
            if (kt) {
              buffer.add(
                  statement(
                      indent(2),
                      "if (input.size < 2 || input[0] != '['.code.toByte() || input[input.size - 1]"
                          + " != ']'.code.toByte()) throw IllegalArgumentException(",
                      string("tRPC body for multiple arguments must be a JSON array (tuple)"),
                      ")"));
            } else {
              buffer.add(
                  statement(
                      indent(2),
                      "if (input.length < 2 || input[0] != '[' || input[input.length - 1] != ']')"
                          + " throw new IllegalArgumentException(",
                      string("tRPC body for multiple arguments must be a JSON array (tuple)"),
                      ");"));
            }
          }
        }

        if (kt) {
          buffer.add(
              statement(
                  indent(2),
                  "parser.reader(input, ",
                  String.valueOf(isTuple),
                  ").use { reader -> "));
        } else {
          buffer.add(
              statement(
                  indent(2),
                  "try (var reader = parser.reader(input, ",
                  String.valueOf(isTuple),
                  ")) {"));
        }

        buffer.addAll(generateTrpcParameter(kt, paramList::add));
      } else if (!isTrpc) {
        for (var parameter : getParameters(true)) {
          String generatedParameter = parameter.generateMapping(kt);
          if (parameter.isRequireBeanValidation()) {
            generatedParameter =
                CodeBlock.of(
                    "io.jooby.validation.BeanValidator.apply(", "ctx, ", generatedParameter, ")");
          }
          paramList.add(generatedParameter);
        }
      }

      controllerVar(kt, buffer, controllerIndent);

      // 2. Resolve Return Flow
      if (returnType.isVoid()) {
        String statusCode =
            annotationMap.size() == 1
                    && annotationMap
                        .keySet()
                        .iterator()
                        .next()
                        .getSimpleName()
                        .toString()
                        .equals("DELETE")
                ? "NO_CONTENT"
                : "OK";

        if (annotationMap.size() == 1) {
          buffer.add(
              statement(
                  indent(controllerIndent),
                  "ctx.setResponseCode(io.jooby.StatusCode.",
                  statusCode,
                  ")",
                  semicolon(kt)));
        } else {
          if (kt) {
            buffer.add(
                statement(
                    indent(controllerIndent),
                    "ctx.setResponseCode(if (ctx.getRoute().getMethod().equals(",
                    string("DELETE"),
                    ")) io.jooby.StatusCode.NO_CONTENT else io.jooby.StatusCode.OK)"));
          } else {
            buffer.add(
                statement(
                    indent(controllerIndent),
                    "ctx.setResponseCode(ctx.getRoute().getMethod().equals(",
                    string("DELETE"),
                    ") ? io.jooby.StatusCode.NO_CONTENT: io.jooby.StatusCode.OK)",
                    semicolon(false)));
          }
        }

        buffer.add(
            statement(
                indent(controllerIndent),
                "c.",
                this.method.getSimpleName(),
                paramList.toString(),
                semicolon(kt)));

        if (isTrpc) {
          buffer.add(
              statement(
                  indent(controllerIndent),
                  "return io.jooby.trpc.TrpcResponse.empty()",
                  semicolon(kt)));
        } else {
          buffer.add(
              statement(indent(controllerIndent), "return ctx.getResponseCode()", semicolon(kt)));
        }
      } else if (returnType.is("io.jooby.StatusCode")) {
        buffer.add(
            statement(
                indent(controllerIndent),
                kt ? "val" : "var",
                " statusCode = c.",
                this.method.getSimpleName(),
                paramList.toString(),
                semicolon(kt)));
        buffer.add(
            statement(indent(controllerIndent), "ctx.setResponseCode(statusCode)", semicolon(kt)));

        if (isTrpc) {
          buffer.add(
              statement(
                  indent(controllerIndent),
                  "return io.jooby.trpc.TrpcResponse.of(statusCode)",
                  semicolon(kt)));
        } else {
          buffer.add(statement(indent(controllerIndent), "return statusCode", semicolon(kt)));
        }
      } else {
        var castStr =
            customReturnType.isProjection()
                ? ""
                : customReturnType.getArgumentsString(kt, false, Set.of(TypeKind.TYPEVAR));

        var needsCast =
            !castStr.isEmpty()
                || (kt
                    && !customReturnType.isProjection()
                    && !customReturnType.getArguments().isEmpty());

        var kotlinNotEnoughTypeInformation = !castStr.isEmpty() && kt ? "<Any>" : "";
        var call =
            of(
                "c.",
                this.method.getSimpleName(),
                kotlinNotEnoughTypeInformation,
                paramList.toString());

        if (needsCast) {
          setUncheckedCast(true);
          call = kt ? call + " as " + returnTypeString : "(" + returnTypeString + ") " + call;
        }

        if (customReturnType.isProjection()) {
          var projected =
              of(Types.PROJECTED, ".wrap(", call, ").include(", string(getProjection()), ")");
          if (isTrpc) {
            buffer.add(
                statement(
                    indent(controllerIndent),
                    "return io.jooby.trpc.TrpcResponse.of(",
                    projected,
                    ")",
                    semicolon(kt)));
          } else {
            buffer.add(
                statement(
                    indent(controllerIndent),
                    "return ",
                    projected,
                    kt && nullable ? "!!" : "",
                    semicolon(kt)));
          }
        } else {

          if (isTrpc && reactive != null) {
            if (isReactiveVoid) {
              // Ensure empty void streams systematically resolve into an empty TrpcResponse
              var handler = reactive.handlerType();
              if (handler.contains("Reactor")) {
                buffer.add(
                    statement(
                        indent(controllerIndent),
                        "return ",
                        call,
                        ".then(reactor.core.publisher.Mono.just(io.jooby.trpc.TrpcResponse.empty()))",
                        semicolon(kt)));
              } else if (handler.contains("Mutiny")) {
                buffer.add(
                    statement(
                        indent(controllerIndent),
                        "return ",
                        call,
                        ".replaceWith(io.jooby.trpc.TrpcResponse.empty())",
                        semicolon(kt)));
              } else if (handler.contains("ReactiveSupport")) {
                buffer.add(
                    statement(
                        indent(controllerIndent),
                        "return ",
                        call,
                        ".thenApply(x -> io.jooby.trpc.TrpcResponse.empty())",
                        semicolon(kt)));
              } else if (handler.contains("Reactivex")) {
                buffer.add(
                    statement(
                        indent(controllerIndent),
                        "return ",
                        call,
                        ".toSingleDefault(io.jooby.trpc.TrpcResponse.empty())",
                        semicolon(kt)));
              } else {
                buffer.add(
                    statement(
                        indent(controllerIndent),
                        "return ",
                        call,
                        ".map(x -> io.jooby.trpc.TrpcResponse.empty())",
                        semicolon(kt)));
              }
            } else {
              buffer.add(
                  statement(
                      indent(controllerIndent),
                      "return ",
                      call,
                      reactive.mapOperator(),
                      semicolon(kt)));
            }
          } else if (isTrpc) {
            buffer.add(
                statement(
                    indent(controllerIndent),
                    "return io.jooby.trpc.TrpcResponse.of(",
                    call,
                    kt && nullable ? "!!" : "",
                    ")",
                    semicolon(kt)));
          } else {
            buffer.add(
                statement(
                    indent(controllerIndent),
                    "return ",
                    call,
                    kt && nullable ? "!!" : "",
                    semicolon(kt)));
          }
        }
      }

      if (isTrpc && !parameters.isEmpty()) {
        buffer.add(statement(indent(2), "}"));
      }

      buffer.add(statement("}", System.lineSeparator()));

      if (uncheckedCast) {
        if (kt) {
          buffer.addFirst(statement("@Suppress(\"UNCHECKED_CAST\")"));
        } else {
          buffer.addFirst(statement("@SuppressWarnings(\"unchecked\")"));
        }
      }
    }
    return buffer;
  }

  private boolean methodCallHeader(
      boolean kt,
      String contextVarname,
      String methodName,
      ArrayList<String> buffer,
      String returnTypeGenerics,
      String returnTypeString,
      boolean throwsException) {
    var nullable = false;
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
                methodName,
                "(handler: io.jooby.kt.HandlerContext): ",
                returnTypeString,
                " {"));
        buffer.add(statement(indent(2), "val ", contextVarname, " = handler.ctx"));
      } else {
        buffer.add(
            statement(
                "fun ",
                returnTypeGenerics,
                methodName,
                "(",
                contextVarname,
                ": io.jooby.Context): ",
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
              "(io.jooby.Context ",
              contextVarname,
              ") ",
              throwsException ? "throws Exception {" : "{"));
    }
    return nullable;
  }

  private List<String> generateTrpcParameter(boolean kt, Consumer<String> arguments) {
    var statements = new ArrayList<String>();
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
              statements.add(
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
            statements.add(
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
          arguments.accept(paramenterName);
          break;
        case "byte",
        "short",
        "float",
        "char",
        "java.lang.Byte",
        "java.lang.Short",
        "java.lang.Float",
        "java.lang.Character":
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
              statements.add(
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
              statements.add(
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
              statements.add(
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
              statements.add(
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
          arguments.accept(paramenterName);
          break;
        default:
          if (kt) {
            statements.add(
                statement(
                    indent(4),
                    "val ",
                    paramenterName,
                    "Decoder: io.jooby.trpc.TrpcDecoder<",
                    type,
                    "> = parser.decoder(",
                    parameter.getType().toSourceCode(kt),
                    ")",
                    semicolon(kt)));
            if (isNullable) {
              statements.add(
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
              statements.add(
                  statement(
                      indent(4),
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
                    indent(4),
                    "io.jooby.trpc.TrpcDecoder<",
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
                      indent(4),
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
                      indent(4),
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

  private void controllerVar(boolean kt, List<String> buffer) {
    controllerVar(kt, buffer, 2);
  }

  private void controllerVar(boolean kt, List<String> buffer, int indent) {
    buffer.add(statement(indent(indent), var(kt), "c = this.factory.apply(ctx)", semicolon(kt)));
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

    // Eagerly flag as tRPC so equals/hashCode can differentiate hybrid methods early
    if (HttpMethod.findByAnnotationName(annotation.getQualifiedName().toString())
        == HttpMethod.tRPC) {
      this.isTrpc = true;
    }
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
    return Objects.hash(method.toString(), isTrpc);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof MvcRoute that) {
      return this.method.toString().equals(that.method.toString()) && this.isTrpc == that.isTrpc;
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

  private HttpMethod trpcMethod(Element element) {
    // 1. High Precedence: Explicit tRPC procedure annotations
    if (AnnotationSupport.findAnnotationByName(element, "io.jooby.annotation.Trpc.Query") != null) {
      return HttpMethod.GET;
    }
    if (AnnotationSupport.findAnnotationByName(element, "io.jooby.annotation.Trpc.Mutation")
        != null) {
      return HttpMethod.POST;
    }

    // 2. Base Precedence: @Trpc combined with standard HTTP annotations
    var trpc = AnnotationSupport.findAnnotationByName(element, "io.jooby.annotation.Trpc");
    if (trpc != null) {
      if (HttpMethod.GET.matches(element)) {
        return HttpMethod.GET;
      }

      // Map all state-changing HTTP annotations to a tRPC POST mutation
      if (HttpMethod.POST.matches(element)
          || HttpMethod.PUT.matches(element)
          || HttpMethod.PATCH.matches(element)
          || HttpMethod.DELETE.matches(element)) {
        return HttpMethod.POST;
      }

      // 3. Fallback: Missing HTTP Method -> Compilation Error
      throw new IllegalArgumentException(
          "tRPC procedure missing HTTP mapping. Method "
              + element.getSimpleName()
              + "() in "
              + element.getEnclosingElement().getSimpleName()
              + " is annotated with @Trpc but lacks a valid HTTP method annotation. Please annotate"
              + " the method with @Trpc.Query, @Trpc.Mutation, or combine @Trpc with @GET, @POST,"
              + " @PUT, @PATCH, or @DELETE.");
    }
    return null;
  }

  public String trpcPath(Element element) {
    var namespace =
        Optional.ofNullable(
                AnnotationSupport.findAnnotationByName(
                    element.getEnclosingElement(), "io.jooby.annotation.Trpc"))
            .flatMap(it -> findAnnotationValue(it, VALUE).stream().findFirst())
            .map(it -> it + ".")
            .orElse("");

    var procedure =
        Stream.of(
                "io.jooby.annotation.Trpc.Query",
                "io.jooby.annotation.Trpc.Mutation",
                "io.jooby.annotation.Trpc")
            .map(it -> AnnotationSupport.findAnnotationByName(element, it))
            .filter(Objects::nonNull)
            .findFirst()
            .flatMap(it -> findAnnotationValue(it, VALUE).stream().findFirst())
            .orElse(element.getSimpleName().toString());
    return Stream.of("trpc", namespace + procedure)
        .map(segment -> segment.startsWith("/") ? segment.substring(1) : segment)
        .collect(Collectors.joining("/", "/", ""));
  }
}
