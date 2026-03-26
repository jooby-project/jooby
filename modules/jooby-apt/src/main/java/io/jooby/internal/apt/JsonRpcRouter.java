/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt;

import static io.jooby.internal.apt.AnnotationSupport.VALUE;
import static io.jooby.internal.apt.CodeBlock.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

public class JsonRpcRouter extends WebRouter<JsonRpcRoute> {

  public JsonRpcRouter(MvcContext context, TypeElement clazz) {
    super(context, clazz);
  }

  public static JsonRpcRouter parse(MvcContext context, TypeElement controller) {
    var router = new JsonRpcRouter(context, controller);
    var classAnnotation =
        AnnotationSupport.findAnnotationByName(controller, "io.jooby.annotation.JsonRpc");

    var explicitlyAnnotated = new ArrayList<ExecutableElement>();
    var allPublicMethods = new ArrayList<ExecutableElement>();

    for (var enclosed : controller.getEnclosedElements()) {
      if (enclosed.getKind() == ElementKind.METHOD) {
        var method = (ExecutableElement) enclosed;
        var modifiers = method.getModifiers();

        if (modifiers.contains(Modifier.PUBLIC)
            && !modifiers.contains(Modifier.STATIC)
            && !modifiers.contains(Modifier.ABSTRACT)) {
          var methodName = method.getSimpleName().toString();
          if (methodName.equals("toString")
              || methodName.equals("hashCode")
              || methodName.equals("equals")
              || methodName.equals("clone")) continue;

          allPublicMethods.add(method);
          if (AnnotationSupport.findAnnotationByName(method, "io.jooby.annotation.JsonRpc")
              != null) {
            explicitlyAnnotated.add(method);
          }
        }
      }
    }

    if (!explicitlyAnnotated.isEmpty()) {
      for (var method : explicitlyAnnotated) {
        var route = new JsonRpcRoute(router, method);
        router.routes.put(route.getMethodName(), route);
      }
    } else if (classAnnotation != null) {
      for (var method : allPublicMethods) {
        var route = new JsonRpcRoute(router, method);
        router.routes.put(route.getMethodName(), route);
      }
    }
    return router;
  }

  @Override
  public String getGeneratedType() {
    return context.generateRouterName(getTargetType().getQualifiedName() + "Rpc");
  }

  private String getJsonRpcNamespace() {
    var annotation = AnnotationSupport.findAnnotationByName(clazz, "io.jooby.annotation.JsonRpc");
    if (annotation != null) {
      return AnnotationSupport.findAnnotationValue(annotation, VALUE).stream()
          .findFirst()
          .orElse("");
    }
    return "";
  }

  @Override
  public String toSourceCode(Boolean generateKotlin) throws IOException {
    boolean kt = generateKotlin == Boolean.TRUE || isKt();
    var generateTypeName = getTargetType().getSimpleName().toString();
    var generatedClass = getGeneratedType().substring(getGeneratedType().lastIndexOf('.') + 1);
    var namespace = getJsonRpcNamespace();

    var template = kt ? KOTLIN : JAVA;
    var buffer = new StringBuilder();

    context.generateStaticImports(
        this,
        (owner, fn) ->
            buffer.append(
                statement("import ", kt ? "" : "static ", owner, ".", fn, semicolon(kt))));
    var imports = buffer.toString();
    buffer.setLength(0);

    List<String> fullMethods = new ArrayList<>();
    for (var route : getRoutes()) {
      var routeName = route.getJsonRpcMethodName();
      fullMethods.add(namespace.isEmpty() ? routeName : namespace + "." + routeName);
    }

    var methodListString =
        fullMethods.stream().map(m -> "\"" + m + "\"").collect(Collectors.joining(", "));

    if (kt) {
      buffer.append(statement(indent(4), "@Throws(Exception::class)"));
      buffer.append(statement(indent(4), "override fun install(app: io.jooby.Jooby) {"));
      buffer.append(
          statement(
              indent(6),
              "app.services.listOf(io.jooby.rpc.jsonrpc.JsonRpcService::class.java).add(this)"));
      buffer.append(statement(indent(4), "}", System.lineSeparator()));

      buffer.append(statement(indent(4), "override fun getMethods(): List<String> {"));
      buffer.append(statement(indent(6), "return listOf(", methodListString, ")"));
      buffer.append(statement(indent(4), "}", System.lineSeparator()));

      buffer.append(
          statement(
              indent(4),
              "override fun execute(ctx: io.jooby.Context, req:"
                  + " io.jooby.rpc.jsonrpc.JsonRpcRequest): Any? {"));
      buffer.append(statement(indent(6), "val c = factory.apply(ctx)"));
      buffer.append(statement(indent(6), "val method = req.method"));
      buffer.append(
          statement(
              indent(6),
              "val parser = ctx.require(io.jooby.rpc.jsonrpc.JsonRpcParser::class.java)"));
      buffer.append(statement(indent(6), "return when(method) {"));

      for (int i = 0; i < getRoutes().size(); i++) {
        buffer.append(statement(indent(8), string(fullMethods.get(i)), " -> {"));
        getRoutes().get(i).generateJsonRpcDispatchCase(true).forEach(buffer::append);
        buffer.append(statement(indent(8), "}"));
      }

      buffer.append(
          statement(
              indent(8),
              "else -> throw"
                  + " io.jooby.rpc.jsonrpc.JsonRpcException(io.jooby.rpc.jsonrpc.JsonRpcErrorCode.METHOD_NOT_FOUND,",
              string("Method not found: $method"),
              ")"));
      buffer.append(statement(indent(6), "}"));
      buffer.append(statement(indent(4), "}"));

    } else {
      buffer.append(statement(indent(4), "@Override"));
      buffer.append(
          statement(indent(4), "public void install(io.jooby.Jooby app) throws Exception {"));
      buffer.append(
          statement(
              indent(6),
              "app.getServices().listOf(io.jooby.rpc.jsonrpc.JsonRpcService.class).add(this);"));
      buffer.append(statement(indent(4), "}", System.lineSeparator()));

      buffer.append(statement(indent(4), "@Override"));
      buffer.append(statement(indent(4), "public java.util.List<String> getMethods() {"));
      buffer.append(statement(indent(6), "return java.util.List.of(", methodListString, ");"));
      buffer.append(statement(indent(4), "}", System.lineSeparator()));

      buffer.append(statement(indent(4), "@Override"));
      buffer.append(
          statement(
              indent(4),
              "public Object execute(io.jooby.Context ctx, io.jooby.rpc.jsonrpc.JsonRpcRequest req)"
                  + " throws Exception {"));
      buffer.append(statement(indent(6), "var c = factory.apply(ctx);"));
      buffer.append(statement(indent(6), "var method = req.getMethod();"));
      buffer.append(
          statement(
              indent(6),
              "io.jooby.rpc.jsonrpc.JsonRpcParser parser ="
                  + " ctx.require(io.jooby.rpc.jsonrpc.JsonRpcParser.class);"));
      buffer.append(statement(indent(6), "switch(method) {"));

      for (int i = 0; i < getRoutes().size(); i++) {
        buffer.append(statement(indent(8), "case ", string(fullMethods.get(i)), ": {"));
        getRoutes().get(i).generateJsonRpcDispatchCase(false).forEach(buffer::append);
        buffer.append(statement(indent(8), "}"));
      }

      buffer.append(statement(indent(8), "default:"));
      buffer.append(
          statement(
              indent(10),
              "throw new"
                  + " io.jooby.rpc.jsonrpc.JsonRpcException(io.jooby.rpc.jsonrpc.JsonRpcErrorCode.METHOD_NOT_FOUND,"
                  + " ",
              string("Method not found:"),
              " + method);"));
      buffer.append(statement(indent(6), "}"));
      buffer.append(statement(indent(4), "}"));
    }

    return template
        .replace("${packageName}", getPackageName())
        .replace("${imports}", imports)
        .replace("${className}", generateTypeName)
        .replace("${generatedClassName}", generatedClass)
        .replace("${implements}", "io.jooby.rpc.jsonrpc.JsonRpcService, io.jooby.Extension")
        .replace("${constructors}", constructors(generatedClass, kt))
        .replace("${methods}", trimr(buffer));
  }
}
