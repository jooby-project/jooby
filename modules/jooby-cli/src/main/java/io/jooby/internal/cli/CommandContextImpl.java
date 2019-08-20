/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.cli;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import io.jooby.cli.CommandContext;
import org.jline.reader.LineReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collections;
import java.util.Set;

public class CommandContextImpl implements CommandContext {

  private final LineReader reader;

  private final Handlebars templates;

  private final PrintWriter out;

  public CommandContextImpl(LineReader reader) {
    this.reader = reader;
    this.out = reader.getTerminal().writer();
    TemplateLoader loader = new ClassPathTemplateLoader("/cli");
    this.templates = new Handlebars(loader);
    this.templates.setPrettyPrint(true);
  }

  @Override public void exit(int code) {
    System.exit(code);
  }

  @Override public String readLine(String prompt) {
    return reader.readLine(prompt);
  }

  @Override public void println(String message) {
    out.println(message);
  }

  @Override public void writeTemplate(String template, Object model, Path file) throws IOException {
    Path parent = file.getParent();
    if (!Files.exists(parent)) {
      Files.createDirectories(parent);
    }
    try (PrintWriter writer = new PrintWriter(file.toFile())) {
      writeTemplate(template, model, writer);
    }
  }

  private void writeTemplate(String template, Object model, Writer writer)
      throws IOException {
    templates.compile(template).apply(model, writer);
  }

  @Override public void copyResource(String source, Path dest) throws IOException {
    copyResource(source, dest, Collections.emptySet());
  }

  @Override public void copyResource(String source, Path dest, Set<PosixFilePermission> permissions)
      throws IOException {
    Path parent = dest.getParent();
    if (!Files.exists(parent)) {
      Files.createDirectories(parent);
    }
    try (InputStream in = getClass().getResourceAsStream(source)) {
      Files.copy(in, dest);
    }

    if (permissions.size() > 0) {
      Files.setPosixFilePermissions(dest, permissions);
    }
  }
}
