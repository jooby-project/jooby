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
import java.io.InputStream;
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

    String appname = application.getClass().getSimpleName()
        .replace("Jooby", "openapi")
        .replace("Kooby", "openapi");
    for (Format ext : format) {
      String filename = String.format("/%s.%s", appname, ext.name().toLowerCase());
      String openAPIFileLocation = Router.normalizePath(dir) + filename;
      application.assets(fullPath(openAPIPath, "/openapi." + ext.name().toLowerCase()),
          openAPIFileLocation);
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
      if (classLoader.getResource(name + "/index.html") != null) {
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

    String template = readString(source, "index.html")
        .replace("${openAPIPath}", openAPIJSON)
        .replace("${redocPath}", fullPath(application.getContextPath(), redocPath));
    application
        .get(redocPath, ctx -> ctx.setResponseType(MediaType.html).send(template));
  }

  private void swaggerUI(Jooby application, AssetSource source) throws IOException {
    String openAPIJSON = fullPath(
        fullPath(application.getContextPath(), openAPIPath), "/openapi.json");

    String template = readString(source, "index.html")
        .replace("${openAPIPath}", openAPIJSON)
        .replace("${swaggerPath}", fullPath(application.getContextPath(), swaggerUIPath));

    application.assets(swaggerUIPath + "/*", source);
    application.get(swaggerUIPath, ctx -> ctx.setResponseType(MediaType.html).send(template));
  }

  private static String readString(AssetSource source, String resource) throws IOException {
    try (InputStream stream = source.resolve(resource).stream()) {
      return IOUtils.toString(stream, "UTF-8");
    }
  }

  private static String fullPath(String contextPath, String path) {
    return Router.noTrailingSlash(Router.normalizePath(contextPath + path));
  }
}
