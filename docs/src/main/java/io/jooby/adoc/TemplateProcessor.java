/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.adoc;

import io.jooby.SneakyThrows;
import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.IncludeProcessor;
import org.asciidoctor.extension.PreprocessorReader;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class TemplateProcessor extends IncludeProcessor {

  @Override
  public void process(Document document, PreprocessorReader reader, String target, Map<String, Object> attributes) {
    var filename = Paths.get("asciidoc", "templates", target.replace(".template", ".adoc"));
    try {
      var content = Files.readString(filename);
      for (var e : attributes.entrySet()) {
        String key = "{{" + e.getKey() + "}}";
        content = content.replace(key, e.getValue().toString());
      }

      reader.pushInclude(content, filename.toString(), filename.toString(), 1, attributes);
    } catch (Exception error) {
      String errorMessage = "Failed to include file: " + filename;
      reader.pushInclude("Error: " + errorMessage, target, target, 1, attributes);
      throw SneakyThrows.propagate(error);
    }
  }

  @Override
  public boolean handles(String target) {
    return target.endsWith(".template");
  }
}
