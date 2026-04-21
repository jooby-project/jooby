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
import java.util.List;

import static io.jooby.internal.apt.CodeBlock.*;
import static java.lang.System.lineSeparator;

public class WsRouter extends WebRouter<WsRoute> {

  public WsRouter(MvcContext context, TypeElement clazz) {
    super(context, clazz);
  }

  public static WsRouter parse(MvcContext context, TypeElement controller) {
    var router = new WsRouter(context, controller);

    for (var enclosed : controller.getEnclosedElements()) {
      if (enclosed.getKind() == ElementKind.METHOD) {
        var route = new WsRoute(router, (ExecutableElement) enclosed);
        if (route.getWsLifecycle() != null) {
          router.routes.put(route.getWsLifecycle().name(), route);
        }
      }
    }

    boolean isWsHandler = router.routes.containsKey(WsLifecycle.CONNECT.name())
                          || router.routes.containsKey(WsLifecycle.MESSAGE.name());

    if (!isWsHandler) {
      return new WsRouter(context, controller);
    }

    return router;
  }

  @Override
  public String getGeneratedType() {
    return context.generateRouterName(getTargetType().getQualifiedName() + "Ws");
  }

  private List<String> websocketPaths() {
    var declared = HttpPath.PATH.path(clazz);
    if (declared.isEmpty()) {
      return List.of("/");
    }

    return declared.stream()
        .map(WebRoute::leadingSlash)
        .distinct()
        .toList();
  }

  @Override
  public String toSourceCode(boolean kt) {
    var generateTypeName = getTargetType().getSimpleName().toString();
    var generatedClass = getGeneratedType().substring(getGeneratedType().lastIndexOf('.') + 1);
    var paths = websocketPaths();

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

    for (var path : paths) {
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
                              WsRoute route,
                              String openLine,
                              String closeToken) {
    buffer.append(indent(6)).append(route.seeControllerMethodJavadoc(kt));
    buffer.append(indent(6)).append(openLine).append(lineSeparator());
    route.appendBody(kt, buffer, indent(8));
    buffer.append(indent(6)).append(closeToken).append(lineSeparator()).append(lineSeparator());
  }
}
