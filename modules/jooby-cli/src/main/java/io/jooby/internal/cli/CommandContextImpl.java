/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.cli;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.jline.reader.LineReader;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import io.jooby.cli.Cli;
import io.jooby.cli.Context;

public class CommandContextImpl implements Context {
  private final LineReader reader;

  private final Handlebars templates;

  private final PrintWriter out;

  private final String version;

  private Map configuration;

  private Properties versions;

  private Path configurationFile;

  public CommandContextImpl(LineReader reader, String version) throws IOException {
    this.reader = reader;
    this.out = reader.getTerminal().writer();
    TemplateLoader loader = new ClassPathTemplateLoader("/cli");
    this.templates = new Handlebars(loader);
    this.templates.setPrettyPrint(true);
    this.version = version;

    // move from .jooby to .config/jooby.conf
    configurationFile = Paths.get(System.getProperty("user.home"), ".config", "jooby.conf");
    migrateOldConfiguration(Paths.get(System.getProperty("user.home"), ".jooby"), configurationFile);

    if (Files.exists(configurationFile)) {
      try (Reader in = Files.newBufferedReader(configurationFile)) {
        configuration = Cli.gson.fromJson(in, LinkedHashMap.class);
      }
    } else {
      configuration = new LinkedHashMap();
    }
  }

  private void migrateOldConfiguration(Path from, Path to) throws IOException {
    if (Files.exists(from)) {
      if (!Files.exists(to.getParent())) {
        Files.createDirectories(to.getParent());
      }
      Files.copy(from, to);
      Files.delete(from);
    }
  }

  @Nonnull @Override public String getVersion() {
    return (String) configuration.getOrDefault("version", version);
  }

  @Nonnull @Override public Path getWorkspace() {
    String workspace = (String) configuration.getOrDefault("workspace",
        System.getProperty("user.dir"));
    return Paths.get(workspace);
  }

  @Override public void setWorkspace(@Nonnull Path workspace) throws IOException {
    if (!Files.isDirectory(workspace)) {
      throw new FileNotFoundException(workspace.toAbsolutePath().toString());
    }
    configuration.put("workspace", workspace.toAbsolutePath().toString());
    String json = Cli.gson.toJson(configuration);
    Files.write(configurationFile, json.getBytes(StandardCharsets.UTF_8));
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
    for (Map.Entry<Object, Object> entry : versions.entrySet()) {
      String key = Stream.of(entry.getKey().toString().split("\\.|-"))
          .map(name -> Character.toUpperCase(name.charAt(0)) + name.substring(1))
          .collect(Collectors.joining());
      key = Character.toLowerCase(key.charAt(0)) + key.substring(1);
      result.put(key, entry.getValue().toString());
    }
    return result;
  }

  @Override public String toString() {
    return "version: " + getVersion() + "; conf: " + configurationFile;
  }
}
