/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt.ws;

import io.jooby.internal.apt.*;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.List;
import java.util.Map;

import static io.jooby.internal.apt.AnnotationSupport.VALUE;
import static io.jooby.internal.apt.AnnotationSupport.findAnnotationByName;
import static io.jooby.internal.apt.CodeBlock.*;
import static java.lang.System.lineSeparator;

public class WsRouter extends WebRouter<WsHandlerMethod> {

  private static final String WEBSOCKET_ROUTE_ANNOTATION = "io.jooby.annotation.ws.WebSocketRoute";

  private static final Map<String, WsLifecycle> LIFECYCLE_ANNOTATIONS =
      Map.of(
          "io.jooby.annotation.ws.OnConnect", WsLifecycle.CONNECT,
          "io.jooby.annotation.ws.OnMessage", WsLifecycle.MESSAGE,
          "io.jooby.annotation.ws.OnClose", WsLifecycle.CLOSE,
          "io.jooby.annotation.ws.OnError", WsLifecycle.ERROR);

  public WsRouter(MvcContext context, TypeElement clazz) {
    super(context, clazz);
  }

  public static WsRouter parse(MvcContext context, TypeElement controller) {
    var router = new WsRouter(context, controller);
    if (findAnnotationByName(controller, WEBSOCKET_ROUTE_ANNOTATION) == null) {
      return router;
    }

    for (var enclosed : controller.getEnclosedElements()) {
      if (enclosed.getKind() != ElementKind.METHOD) {
        continue;
      }

      var method = (ExecutableElement) enclosed;
      for (var mirror : method.getAnnotationMirrors()) {
        var annoName = mirror.getAnnotationType().asElement().toString();
        var lc = LIFECYCLE_ANNOTATIONS.get(annoName);
        if (lc == null) {
          continue;
        }

        var key = lc.name();
        if (router.routes.containsKey(key)) {
          context.error(
              "Duplicate websocket lifecycle annotation %s on type %s",
              annoName,
              controller.getQualifiedName());
          continue;
        }
        validateLifecycleParameters(context, method, lc);
        router.routes.put(key, new WsHandlerMethod(router, method));
      }
    }

    if (router.routes.isEmpty()) {
      context.error(
          "Websocket handler %s must declare at least one of @OnConnect, @OnMessage, @OnClose, @OnError",
          controller.getQualifiedName());
    }
    return router;
  }

  private static void validateLifecycleParameters(MvcContext context,
                                                  ExecutableElement method,
                                                  WsLifecycle lc) {
    var env = context.getProcessingEnvironment();
    var types = env.getTypeUtils();
    var throwableType = env.getElementUtils().getTypeElement(Throwable.class.getName()).asType();
    var allowed = WsParamTypes.getAllowedTypes(lc);

    for (VariableElement parameter : method.getParameters()) {
      TypeMirror rawMirror = websocketParameterRawType(types, parameter);
      var raw = rawMirror.toString();
      if (allowed.contains(raw)) {
        continue;
      }

      if (lc == WsLifecycle.ERROR
          && throwableType != null
          && types.isAssignable(rawMirror, throwableType)) {
        continue;
      }

      context.error(
          "Illegal parameter type %s on websocket %s method %s#%s",
          raw,
          lc.name().toLowerCase(),
          ((TypeElement) method.getEnclosingElement()).getQualifiedName(),
          method.getSimpleName());
    }
  }

  private static TypeMirror websocketParameterRawType(Types types, VariableElement parameter) {
    return new TypeDefinition(types, parameter.asType()).getRawType();
  }

  @Override
  public String getGeneratedType() {
    return context.generateRouterName(getTargetType().getQualifiedName() + "Ws");
  }

  private List<String> websocketRoutes() {
    var wsMirror = findAnnotationByName(clazz, WEBSOCKET_ROUTE_ANNOTATION);
    if (wsMirror == null) {
      return List.of();
    }

    var paths = AnnotationSupport.findAnnotationValue(wsMirror, VALUE);
    if (paths.isEmpty()) {
      paths = List.of("/");
    }
    return paths.stream()
        .map(WebRoute::leadingSlash)
        .distinct()
        .toList();
  }

  @Override
  public String toSourceCode(boolean kt) {
    var generateTypeName = getTargetType().getSimpleName().toString();
    var generatedClass = getGeneratedType().substring(getGeneratedType().lastIndexOf('.') + 1);
    var routes = websocketRoutes();

    var buffer = new StringBuilder();

    if (kt) {
      buffer.append(indent(4)).append("@Throws(Exception::class)").append(lineSeparator());
      buffer
          .append(indent(4))
          .append("override fun install(app: io.jooby.Jooby) {")
          .append(lineSeparator());
    } else {
      buffer
          .append(indent(4))
          .append("public void install(io.jooby.Jooby app) throws Exception {")
          .append(lineSeparator());
    }

    for (var path : routes) {
      buffer.append(
          statement(
              indent(6),
              "app.ws(",
              CodeBlock.string(path),
              ", ",
              "this::wsInit",
              ")",
              semicolon(kt))
      );
    }

    trimr(buffer);
    buffer.append(lineSeparator()).append(indent(4)).append("}").append(lineSeparator());

    buffer.append(lineSeparator()).append(generateWsInitMethod(kt));

    return getTemplate(kt)
        .replace("${packageName}", getPackageName())
        .replace("${imports}", "")
        .replace("${className}", generateTypeName)
        .replace("${generatedClassName}", generatedClass)
        .replace("${implements}", "io.jooby.Extension")
        .replace("${constructors}", constructors(generatedClass, kt))
        .replace("${methods}", trimr(buffer));
  }

  private String generateWsInitMethod(boolean kt) {
    var buffer = new StringBuilder();
    if (!kt) {
      buffer.append(
          statement(
              indent(4),
              "private void wsInit(io.jooby.Context ctx, io.jooby.WebSocketConfigurer"
              + " configurer) {"));
    } else {
      buffer.append(
          statement(
              indent(4),
              "private fun wsInit(ctx: io.jooby.Context, configurer:"
              + " io.jooby.WebSocketConfigurer) {"));
    }

    appendLifecycle(kt, buffer, WsLifecycle.CONNECT);
    appendLifecycle(kt, buffer, WsLifecycle.MESSAGE);
    appendLifecycle(kt, buffer, WsLifecycle.CLOSE);
    appendLifecycle(kt, buffer, WsLifecycle.ERROR);

    buffer.append(indent(4)).append("}").append(lineSeparator());
    return buffer.toString();
  }

  private void appendLifecycle(boolean kt, StringBuilder buffer, WsLifecycle lc) {
    var handler = routes.get(lc.name());
    if (handler == null) {
      return;
    }

    var open =
        switch (lc) {
          case CONNECT -> kt ? "configurer.onConnect { ws ->" : "configurer.onConnect(ws -> {";
          case MESSAGE -> kt
              ? "configurer.onMessage { ws, message ->"
              : "configurer.onMessage((ws, message) -> {";
          case CLOSE ->
              kt ? "configurer.onClose { ws, status ->" : "configurer.onClose((ws, status) -> {";
          case ERROR ->
              kt ? "configurer.onError { ws, cause ->" : "configurer.onError((ws, cause) -> {";
        };
    appendCallback(kt, buffer, handler, open, kt ? "}" : "});");
  }

  private void appendCallback(boolean kt,
                              StringBuilder buffer,
                              WsHandlerMethod handler,
                              String openLine,
                              String closeToken) {
    buffer.append(indent(6)).append(handler.seeControllerMethodJavadoc(kt));
    buffer.append(indent(6)).append(openLine).append(lineSeparator());
    handler.appendBody(kt, buffer, indent(8));
    buffer.append(indent(6)).append(closeToken).append(lineSeparator()).append(lineSeparator());
  }
}
