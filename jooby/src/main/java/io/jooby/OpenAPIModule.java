/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import io.jooby.SneakyThrows.Consumer2;
import org.apache.commons.io.IOUtils;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class OpenAPIModule implements Extension {

  public enum Format {
    JSON,
    YAML
  }

  private final String openAPIPath;
  private String swaggerUIPath = "/swagger";
  private String redocPath = "/redoc";
  private EnumSet<Format> format = EnumSet.of(Format.JSON, Format.YAML);

  public OpenAPIModule(String path) {
    this.openAPIPath = Router.normalizePath(path);
  }

  public OpenAPIModule() {
    this("/");
  }

  public OpenAPIModule swaggerUI(String path) {
    this.swaggerUIPath = Router.normalizePath(path);
    return this;
  }

  public OpenAPIModule redoc(String path) {
    this.redocPath = Router.normalizePath(path);
    return this;
  }

  public OpenAPIModule format(Format... format) {
    this.format = EnumSet.copyOf(Arrays.asList(format));
    return this;
  }

  @Override public void install(@Nonnull Jooby application) throws Exception {
    String dir = Optional.ofNullable(application.getClass().getPackage())
        .map(Package::getName)
        .orElse("/")
        .replace(".", "/");

    for (Format ext : format) {
      String filename = "/openapi." + ext.name().toLowerCase();
      String openAPIFileLocation = Router.normalizePath(dir) + filename;
      application.assets(fullPath(openAPIPath, filename), openAPIFileLocation);
    }

    /** Configure UI: */
    configureUI(application);
  }

  private void configureUI(@Nonnull Jooby application) {
    Map<String, Consumer2<Jooby, AssetSource>> ui = new HashMap<>();
    ui.put("swagger-ui", this::swaggerUI);
    ui.put("redoc", this::redoc);
    ClassLoader classLoader = application.getClassLoader();
    for (Map.Entry<String, Consumer2<Jooby, AssetSource>> e : ui.entrySet()) {
      String name = e.getKey();
      if (classLoader.getResource(name) != null) {
        if (format.contains(Format.JSON)) {
          Consumer2<Jooby, AssetSource> consumer = e.getValue();
          consumer.accept(application, AssetSource.create(classLoader, name));
        } else {
          application.getLog().debug("{} is disabled when json format is not supported", name);
        }
      }
    }
  }

  private void redoc(Jooby application, AssetSource source) throws IOException {
    application.assets(redocPath + "/*", source);
    String openAPIJSON = fullPath(
        fullPath(application.getContextPath(), openAPIPath), "/openapi.json");

    String template = IOUtils.toString(source.resolve("index.html").stream(), "UTF-8")
        .replace("${openAPIPath}", openAPIJSON)
        .replace("${redocPath}", fullPath(application.getContextPath(), redocPath));
    application
        .get(redocPath, ctx -> ctx.setResponseType(MediaType.html).send(template));
  }

  private void swaggerUI(Jooby application, AssetSource source) throws IOException {
    String template = swaggerTemplate(source,
        fullPath(application.getContextPath(), swaggerUIPath),
        fullPath(application.getContextPath(), openAPIPath));

    application.assets(swaggerUIPath + "/*", source);
    application.get(swaggerUIPath, ctx -> ctx.setResponseType(MediaType.html).send(template));
  }

  static String swaggerTemplate(AssetSource source, String swaggerPath, String openAPIPath)
      throws IOException {
    return IOUtils
        .toString(source.resolve("index.html").stream(), "UTF-8")
        .replace("${swaggerPath}", swaggerPath)
        .replace("${openAPIPath}", fullPath(openAPIPath, "/openapi.json"));
  }

  private static String fullPath(String contextPath, String path) {
    return Router.noTrailingSlash(Router.normalizePath(contextPath + path));
  }
}
