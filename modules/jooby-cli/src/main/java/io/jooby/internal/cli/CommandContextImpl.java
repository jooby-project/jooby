/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.cli;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import io.jooby.cli.Context;
import org.jline.reader.LineReader;
import org.json.JSONObject;
import org.json.JSONTokener;

import javax.annotation.Nonnull;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class CommandContextImpl implements Context {

  private final LineReader reader;

  private final Handlebars templates;

  private final PrintWriter out;

  private final String version;

  private JSONObject configuration;

  private Properties versions;

  public CommandContextImpl(LineReader reader, String version) throws IOException {
    this.reader = reader;
    this.out = reader.getTerminal().writer();
    TemplateLoader loader = new ClassPathTemplateLoader("/cli");
    this.templates = new Handlebars(loader);
    this.templates.setPrettyPrint(true);
    this.version = version;

    Path file = configurationPath();

    if (Files.exists(file)) {
      configuration = new JSONObject(new JSONTokener(Files.newBufferedReader(file)));
    } else {
      configuration = new JSONObject();
    }
  }

  private Path configurationPath() {
    return Paths.get(System.getProperty("user.home"), ".jooby");
  }

  @Nonnull @Override public String getVersion() {
    return configuration.has("version") ? configuration.getString("version") : version;
  }

  @Nonnull @Override public Path getWorkspace() {
    return configuration.has("workspace")
        ? Paths.get(configuration.getString("workspace"))
        : Paths.get(System.getProperty("user.dir"));
  }

  @Override public void setWorkspace(@Nonnull Path workspace) throws IOException {
    if (!Files.isDirectory(workspace)) {
      throw new FileNotFoundException(workspace.toAbsolutePath().toString());
    }
    configuration.put("workspace", workspace.toAbsolutePath().toString());
    Files.write(configurationPath(), configuration.toString().getBytes(StandardCharsets.UTF_8));

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
      try {
        Files.setPosixFilePermissions(dest, permissions);
      } catch (UnsupportedOperationException x) {
        // Windows OS, ignore it.
      }
    }
  }

  public Map<String, String> getDependencyMap() throws IOException {
    if (versions == null) {
      versions = new Properties();
      try (InputStream in = getClass().getResourceAsStream("/dependencies.properties")) {
        versions.load(in);
      }
    }
    Map result = new LinkedHashMap<>();
    result.putAll(versions);
    return result;
  }
}
