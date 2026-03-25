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
    JsonRpcRouter router = new JsonRpcRouter(context, controller);
    var classJsonRpcAnno =
        AnnotationSupport.findAnnotationByName(controller, "io.jooby.annotation.JsonRpc");

    List<ExecutableElement> explicitlyAnnotated = new ArrayList<>();
    List<ExecutableElement> allPublicMethods = new ArrayList<>();

    for (var enclosed : controller.getEnclosedElements()) {
      if (enclosed.getKind() == ElementKind.METHOD) {
        var method = (ExecutableElement) enclosed;
        var modifiers = method.getModifiers();

        if (modifiers.contains(Modifier.PUBLIC)
            && !modifiers.contains(Modifier.STATIC)
            && !modifiers.contains(Modifier.ABSTRACT)) {
          String methodName = method.getSimpleName().toString();
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
        JsonRpcRoute route = new JsonRpcRoute(router, method);
        router.routes.put(route.getMethodName(), route);
      }
    } else if (classJsonRpcAnno != null) {
      for (var method : allPublicMethods) {
        JsonRpcRoute route = new JsonRpcRoute(router, method);
        router.routes.put(route.getMethodName(), route);
      }
    }
    return router;
  }

  @Override
  public String getGeneratedType() {
    return context.generateRouterName(getTargetType().getQualifiedName().toString() + "Rpc");
  }

  public String getJsonRpcNamespace() {
    var annotation = AnnotationSupport.findAnnotationByName(clazz, "io.jooby.annotation.JsonRpc");
    if (annotation != null) {
      return AnnotationSupport.findAnnotationValue(annotation, VALUE).stream()
          .findFirst()
          .orElse("");
    }
    return "";
  }

  @Override
  public String getSourceCode(Boolean generateKotlin) throws IOException {
    if (isEmpty()) return null;

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
    for (JsonRpcRoute route : getRoutes()) {
      String routeName = route.getJsonRpcMethodName();
      fullMethods.add(namespace.isEmpty() ? routeName : namespace + "." + routeName);
    }

    String methodListString =
        fullMethods.stream().map(m -> "\"" + m + "\"").collect(Collectors.joining(", "));

    if (kt) {
      buffer.append(indent(4)).append("@Throws(Exception::class)").append(System.lineSeparator());
      buffer
          .append(indent(4))
          .append("override fun install(app: io.jooby.Jooby) {")
          .append(System.lineSeparator());
      buffer
          .append(indent(6))
          .append("app.services.listOf(io.jooby.rpc.jsonrpc.JsonRpcService::class.java).add(this)")
          .append(System.lineSeparator());
      buffer
          .append(indent(4))
          .append("}")
          .append(System.lineSeparator())
          .append(System.lineSeparator());

      buffer
          .append(indent(4))
          .append("override fun getMethods(): List<String> {")
          .append(System.lineSeparator());
      buffer
          .append(indent(6))
          .append("return listOf(")
          .append(methodListString)
          .append(")")
          .append(System.lineSeparator());
      buffer
          .append(indent(4))
          .append("}")
          .append(System.lineSeparator())
          .append(System.lineSeparator());

      buffer
          .append(indent(4))
          .append(
              "override fun execute(ctx: io.jooby.Context, req:"
                  + " io.jooby.rpc.jsonrpc.JsonRpcRequest): Any? {")
          .append(System.lineSeparator());
      buffer.append(indent(6)).append("val c = factory.apply(ctx)").append(System.lineSeparator());
      buffer.append(indent(6)).append("val method = req.method").append(System.lineSeparator());
      buffer
          .append(indent(6))
          .append("val parser = ctx.require(io.jooby.rpc.jsonrpc.JsonRpcParser::class.java)")
          .append(System.lineSeparator());
      buffer.append(indent(6)).append("return when(method) {").append(System.lineSeparator());

      for (int i = 0; i < getRoutes().size(); i++) {
        buffer
            .append(indent(8))
            .append("\"")
            .append(fullMethods.get(i))
            .append("\" -> {")
            .append(System.lineSeparator());
        getRoutes().get(i).generateJsonRpcDispatchCase(true).forEach(buffer::append);
        buffer.append(indent(8)).append("}").append(System.lineSeparator());
      }

      buffer
          .append(indent(8))
          .append(
              "else -> throw"
                  + " io.jooby.rpc.jsonrpc.JsonRpcException(io.jooby.rpc.jsonrpc.JsonRpcErrorCode.METHOD_NOT_FOUND,"
                  + " \"Method not found: $method\")")
          .append(System.lineSeparator());
      buffer.append(indent(6)).append("}").append(System.lineSeparator());
      buffer.append(indent(4)).append("}").append(System.lineSeparator());

    } else {
      buffer.append(indent(4)).append("@Override").append(System.lineSeparator());
      buffer
          .append(indent(4))
          .append("public void install(io.jooby.Jooby app) throws Exception {")
          .append(System.lineSeparator());
      buffer
          .append(indent(6))
          .append("app.getServices().listOf(io.jooby.rpc.jsonrpc.JsonRpcService.class).add(this);")
          .append(System.lineSeparator());
      buffer
          .append(indent(4))
          .append("}")
          .append(System.lineSeparator())
          .append(System.lineSeparator());

      buffer.append(indent(4)).append("@Override").append(System.lineSeparator());
      buffer
          .append(indent(4))
          .append("public java.util.List<String> getMethods() {")
          .append(System.lineSeparator());
      buffer
          .append(indent(6))
          .append("return java.util.List.of(")
          .append(methodListString)
          .append(");")
          .append(System.lineSeparator());
      buffer
          .append(indent(4))
          .append("}")
          .append(System.lineSeparator())
          .append(System.lineSeparator());

      buffer.append(indent(4)).append("@Override").append(System.lineSeparator());
      buffer
          .append(indent(4))
          .append(
              "public Object execute(io.jooby.Context ctx, io.jooby.rpc.jsonrpc.JsonRpcRequest req)"
                  + " throws Exception {")
          .append(System.lineSeparator());
      buffer
          .append(indent(6))
          .append(generateTypeName)
          .append(" c = factory.apply(ctx);")
          .append(System.lineSeparator());
      buffer
          .append(indent(6))
          .append("String method = req.getMethod();")
          .append(System.lineSeparator());
      buffer
          .append(indent(6))
          .append(
              "io.jooby.rpc.jsonrpc.JsonRpcParser parser ="
                  + " ctx.require(io.jooby.rpc.jsonrpc.JsonRpcParser.class);")
          .append(System.lineSeparator());
      buffer.append(indent(6)).append("switch(method) {").append(System.lineSeparator());

      for (int i = 0; i < getRoutes().size(); i++) {
        buffer
            .append(indent(8))
            .append("case \"")
            .append(fullMethods.get(i))
            .append("\": {")
            .append(System.lineSeparator());
        getRoutes().get(i).generateJsonRpcDispatchCase(false).forEach(buffer::append);
        buffer.append(indent(8)).append("}").append(System.lineSeparator());
      }

      buffer.append(indent(8)).append("default:").append(System.lineSeparator());
      buffer
          .append(indent(10))
          .append(
              "throw new"
                  + " io.jooby.rpc.jsonrpc.JsonRpcException(io.jooby.rpc.jsonrpc.JsonRpcErrorCode.METHOD_NOT_FOUND,"
                  + " \"Method not found: \" + method);")
          .append(System.lineSeparator());
      buffer.append(indent(6)).append("}").append(System.lineSeparator());
      buffer.append(indent(4)).append("}").append(System.lineSeparator());
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
