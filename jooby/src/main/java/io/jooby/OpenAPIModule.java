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

/**
 * OpenAPI supports for Jooby. Basic Usage:
 *
 * <pre>{@code
 * {
 *   install(new OpenAPIModule());
 * }
 * }</pre>
 *
 * The <code>[openapi].json</code> and/or <code>[openapi].yaml</code> files must be present on
 * classpath.
 *
 * If <code>jooby-swagger-ui</code> is present (part of your project classpath) swagger-ui will
 * be available. Same for <code>jooby-redoc</code>.
 *
 * Complete documentation is available at: https://jooby.io/modules/openapi
 *
 * @author edgar
 * @since 2.7.0
 */
public class OpenAPIModule implements Extension {

  /**
   * Available formats.
   */
  public enum Format {
    /**
     * JSON.
     */
    JSON,

    /**
     * YAML.
     */
    YAML
  }

  private final String openAPIPath;
  private String swaggerUIPath = "/swagger";
  private String redocPath = "/redoc";
  private EnumSet<Format> format = EnumSet.of(Format.JSON, Format.YAML);

  /**
   * Creates an OpenAPI module. The path is used to route the open API files. For example:
   *
   * <pre>{@code
   *   install(new OpenAPIModule("/docs"));
   * }</pre>
   *
   * Files will be at <code>/docs/openapi.json</code>, <code>/docs/openapi.yaml</code>.
   *
   * @param path Custom path to use.
   */
  public OpenAPIModule(@Nonnull String path) {
    this.openAPIPath = Router.normalizePath(path);
  }

  /**
   * Creates an OpenAPI module.
   *
   * Files will be at <code>/openapi.json</code>, <code>/openapi.yaml</code>.
   */
  public OpenAPIModule() {
    this("/");
  }

  /**
   * Customize the swagger-ui path. Defaults is <code>/swagger</code>.
   *
   * @param path Swagger-ui path.
   * @return This module.
   */
  public @Nonnull OpenAPIModule swaggerUI(@Nonnull String path) {
    this.swaggerUIPath = Router.normalizePath(path);
    return this;
  }

  /**
   * Customize the redoc-ui path. Defaults is <code>/redoc</code>.
   *
   * @param path Redoc path.
   * @return This module.
   */
  public @Nonnull OpenAPIModule redoc(@Nonnull String path) {
    this.redocPath = Router.normalizePath(path);
    return this;
  }

  /**
   * Enable what format are available (json or yaml).
   *
   * IMPORTANT: UI tools requires the JSON format.
   *
   * @param format Supported formats.
   * @return This module.
   */
  public @Nonnull OpenAPIModule format(@Nonnull Format... format) {
    this.format = EnumSet.copyOf(Arrays.asList(format));
    return this;
  }

  @Override public void install(@Nonnull Jooby application) throws Exception {
    String dir = Optional.ofNullable(application.getBasePackage())
        .orElse("/")
        .replace(".", "/");

    String appname = application.getName()
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

  private void configureUI(Jooby application) {
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
