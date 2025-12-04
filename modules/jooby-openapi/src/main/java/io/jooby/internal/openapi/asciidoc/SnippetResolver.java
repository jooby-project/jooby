/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.asciidoc;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import io.jooby.SneakyThrows;
import io.pebbletemplates.pebble.PebbleEngine;

public class SnippetResolver {
  private final Path baseDir;
  private PebbleEngine engine;

  public SnippetResolver(Path baseDir) {
    this.baseDir = baseDir;
  }

  public String apply(String snippet, Map<String, Object> context) throws IOException {
    var writer = new StringWriter();
    var snippetContent = resolve(baseDir, snippet).trim().replaceAll("\r\n", "\n");
    var template = engine.getLiteralTemplate(snippetContent);
    template.evaluate(writer, context);
    return writer.toString();
  }

  private String resolve(Path snippetDir, String name) {
    try {
      var templatePath = snippetDir.resolve(name + ".snippet");
      if (Files.exists(templatePath)) {
        return Files.readString(templatePath);
      } else {
        var path = "/io/jooby/openapi/templates/asciidoc/default-" + name + ".snippet";
        try (var in = getClass().getResourceAsStream(path)) {
          if (in == null) {
            throw new FileNotFoundException("classpath:" + path);
          }
          return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
      }
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  public void setEngine(PebbleEngine engine) {
    this.engine = engine;
  }
}
