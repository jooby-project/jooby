/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.asciidoc;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.StringWriter;

import org.assertj.core.api.AbstractStringAssert;

import io.jooby.SneakyThrows;
import io.jooby.internal.openapi.OpenAPIExt;
import io.jooby.openapi.CurrentDir;
import io.swagger.v3.core.util.Json31;
import io.swagger.v3.core.util.Yaml31;

public class PebbleTemplateSupport {

  private final AsciiDocContext context;

  public PebbleTemplateSupport(OpenAPIExt openapi) {
    var baseDir = CurrentDir.testClass(getClass());
    var outDir = CurrentDir.basedir("target").resolve("asciidoc-out");
    var classLoader = getClass().getClassLoader();
    this.context =
        new AsciiDocContext(
            classLoader, Json31.mapper(), Yaml31.mapper(), openapi, baseDir, outDir);
  }

  public AbstractStringAssert<?> evaluateThat(String input) throws IOException {
    return assertThat(evaluate(input));
  }

  public void evaluate(String input, SneakyThrows.Consumer<String> consumer) throws IOException {
    consumer.accept(evaluate(input));
  }

  public String evaluate(String input) throws IOException {
    var template = context.getEngine().getLiteralTemplate(input);
    var writer = new StringWriter();
    template.evaluate(writer);
    return writer.toString();
  }
}
