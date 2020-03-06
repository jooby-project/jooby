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
import java.util.function.BiConsumer;

public class OpenAPIModule implements Extension {

  public enum Format {
    JSON,
    YAML
  }

  private static final String REDOC = "<!DOCTYPE html>\n"
      + "<html>\n"
      + "  <head>\n"
      + "    <title>ReDoc</title>\n"
      + "    <!-- needed for adaptive design -->\n"
      + "    <meta charset=\"utf-8\"/>\n"
      + "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n"
      + "    <link href=\"https://fonts.googleapis.com/css?family=Montserrat:300,400,700|Roboto:300,400,700\" rel=\"stylesheet\">\n"
      + "\n"
      + "    <!--\n"
      + "    ReDoc doesn't change outer page styles\n"
      + "    -->\n"
      + "    <style>\n"
      + "      body {\n"
      + "        margin: 0;\n"
      + "        padding: 0;\n"
      + "      }\n"
      + "    </style>\n"
      + "  </head>\n"
      + "  <body>\n"
      + "    <redoc spec-url='${openAPIPath}'></redoc>\n"
      + "    <script src=\"${redocPath}/bundles/redoc.standalone.js\"> </script>\n"
      + "  </body>\n"
      + "</html>";

  private static final String OPENAPI_PATH = "https://petstore.swagger.io/v2/swagger.json";

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
    for (Map.Entry<String, Consumer2<Jooby, AssetSource>> e : ui.entrySet()) {
      String name = e.getKey();
      Optional<AssetSource> source = assetSource(application.getClassLoader(), name);
      if (source.isPresent()) {
        if (format.contains(Format.JSON)) {
          Consumer2<Jooby, AssetSource> consumer = e.getValue();
          consumer.accept(application, source.get());
        } else {
          application.getLog().debug("{} is disabled when json format is not supported", name);
        }
      }
    }
  }

  private Optional<AssetSource> assetSource(ClassLoader loader, String name) {
    try {
      return Optional.of(AssetSource.webjars(loader, name));
    } catch (Exception x) {
      return Optional.empty();
    }
  }

  private void redoc(Jooby application, AssetSource source) throws IOException {
    application.assets(redocPath + "/*", source);
    String openAPIJSON = fullPath(
        fullPath(application.getContextPath(), openAPIPath), "/openapi.json");
    String template = REDOC
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

  static String swaggerTemplate(AssetSource source, String contextPath, String openAPIPath)
      throws IOException {
    String template = IOUtils
        .toString(source.resolve("index.html").stream(), "UTF-8")
        .replace("./", contextPath + "/");
    if (template.contains(OPENAPI_PATH)) {
      return template.replace(OPENAPI_PATH, fullPath(openAPIPath, "/openapi.json"));
    } else {
      throw new IllegalStateException("Unable to find openAPI path from template");
    }
  }

  private static String fullPath(String contextPath, String path) {
    return Router.noTrailingSlash(Router.normalizePath(contextPath + path));
  }
}
