/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.SneakyThrows.Consumer2;
import io.jooby.handler.Asset;
import io.jooby.handler.AssetSource;
import io.jooby.internal.IOUtils;

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
 * <p>If <code>jooby-swagger-ui</code> is present (part of your project classpath) swagger-ui will
 * be available. Same for <code>jooby-redoc</code>.
 *
 * <p>Complete documentation is available at: https://jooby.io/modules/openapi
 *
 * @author edgar
 * @since 2.7.0
 */
public class OpenAPIModule implements Extension {

  private static class OpenAPIAsset implements Asset {

    private long lastModified;

    private byte[] content;

    private MediaType type;

    OpenAPIAsset(MediaType type, byte[] content, long lastModified) {
      this.content = content;
      this.type = type;
      this.lastModified = lastModified;
    }

    @Override
    public long getSize() {
      return content.length;
    }

    @Override
    public long getLastModified() {
      return lastModified;
    }

    @Override
    public boolean isDirectory() {
      return false;
    }

    @NonNull @Override
    public MediaType getContentType() {
      return type;
    }

    @Override
    public InputStream stream() {
      return new ByteArrayInputStream(content);
    }

    @Override
    public void close() throws Exception {
      // NOOP
    }
  }

  private static class OpenAPISource implements AssetSource {

    private Map<String, Asset> assets = new HashMap<>();

    public OpenAPISource put(String key, Asset asset) {
      assets.put(key, asset);
      return this;
    }

    @Nullable @Override
    public Asset resolve(@NonNull String path) {
      return assets.get(path);
    }
  }

  /** Available formats. */
  public enum Format {
    /** JSON. */
    JSON,

    /** YAML. */
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
   * install(new OpenAPIModule("/docs"));
   * }</pre>
   *
   * Files will be at <code>/docs/openapi.json</code>, <code>/docs/openapi.yaml</code>.
   *
   * @param path Custom path to use.
   */
  public OpenAPIModule(@NonNull String path) {
    this.openAPIPath = Router.normalizePath(path);
  }

  /**
   * Creates an OpenAPI module.
   *
   * <p>Files will be at <code>/openapi.json</code>, <code>/openapi.yaml</code>.
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
  public @NonNull OpenAPIModule swaggerUI(@NonNull String path) {
    this.swaggerUIPath = Router.normalizePath(path);
    return this;
  }

  /**
   * Customize the redoc-ui path. Defaults is <code>/redoc</code>.
   *
   * @param path Redoc path.
   * @return This module.
   */
  public @NonNull OpenAPIModule redoc(@NonNull String path) {
    this.redocPath = Router.normalizePath(path);
    return this;
  }

  /**
   * Enable what format are available (json or yaml).
   *
   * <p>IMPORTANT: UI tools requires the JSON format.
   *
   * @param format Supported formats.
   * @return This module.
   */
  public @NonNull OpenAPIModule format(@NonNull Format... format) {
    this.format = EnumSet.copyOf(Arrays.asList(format));
    return this;
  }

  @Override
  public void install(@NonNull Jooby application) throws Exception {
    String dir = Optional.ofNullable(application.getBasePackage()).orElse("/").replace(".", "/");

    String appname = application.getName().replace("Jooby", "openapi").replace("Kooby", "openapi");
    for (Format ext : format) {
      String filename = String.format("/%s.%s", appname, ext.name().toLowerCase());
      String openAPIFileLocation = Router.normalizePath(dir) + filename;
      application.assets(
          fullPath(openAPIPath, "/openapi." + ext.name().toLowerCase()), openAPIFileLocation);
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

  private void redoc(Jooby application, AssetSource source) throws Exception {

    String openAPIJSON =
        fullPath(fullPath(application.getContextPath(), openAPIPath), "/openapi.json");

    AssetSource customSource =
        new OpenAPISource()
            .put(
                "index.html",
                processAsset(
                    source,
                    MediaType.html,
                    "index.html",
                    "${openAPIPath}",
                    openAPIJSON,
                    "${redocPath}",
                    fullPath(application.getContextPath(), redocPath)));

    application.assets(redocPath + "/?*", customSource, source);
  }

  private void swaggerUI(Jooby application, AssetSource source) throws Exception {
    String openAPIJSON =
        fullPath(fullPath(application.getContextPath(), openAPIPath), "/openapi.json");

    AssetSource customSource =
        new OpenAPISource()
            .put(
                "index.html",
                processAsset(
                    source,
                    MediaType.html,
                    "index.html",
                    "${swaggerPath}",
                    fullPath(application.getContextPath(), swaggerUIPath)))
            .put(
                "swagger-initializer.js",
                processAsset(
                    source,
                    MediaType.html,
                    "swagger-initializer.js",
                    "${openAPIPath}",
                    openAPIJSON));
    application.assets(swaggerUIPath + "/?*", customSource, source);
  }

  private static Asset processAsset(
      AssetSource source, MediaType type, String resource, String... replacements)
      throws Exception {
    try (Asset asset = source.resolve(resource)) {
      String content = IOUtils.toString(asset.stream(), StandardCharsets.UTF_8);
      for (int i = 0; i < replacements.length; i += 2) {
        content = content.replace(replacements[i], replacements[i + 1]);
      }
      return new OpenAPIAsset(
          type, content.getBytes(StandardCharsets.UTF_8), asset.getLastModified());
    }
  }

  private static String fullPath(String contextPath, String path) {
    return Router.noTrailingSlash(Router.normalizePath(contextPath + path));
  }
}
