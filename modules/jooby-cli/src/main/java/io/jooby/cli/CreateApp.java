/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.cli;

import io.jooby.SneakyThrows;
import picocli.CommandLine;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@CommandLine.Command(name = "create", description = "Creates a new application")
public class CreateApp extends Command implements Runnable {
  @CommandLine.Parameters
  private String name;

  @CommandLine.Option(names = {"-m", "--maven"}, defaultValue = "true")
  private boolean maven;

  @CommandLine.Option(names = {"-g", "--gradle"})
  private boolean gradle;

  @CommandLine.Option(names = {"-kt", "--kotlin"}, description = "Generate a Kotlin application")
  private boolean kotlin;

  @Override public void run(CommandContext ctx) throws Exception {
    Path projectDir = Paths.get(System.getProperty("user.dir"), name);
    if (Files.exists(projectDir)) {
      throw new IOException("Project directory already exists: " + projectDir);
    }
    Files.createDirectory(projectDir);
    String templateName = maven ? "maven" : "gradle";
    String buildFileName = maven ? "pom.xml" : "build.gradle";
    String packageName = "app";
    String language =  kotlin? "kotlin" : "java";
    String extension =  kotlin? "kt" : "java";

    Map<String, Object> model = new HashMap<>();
    model.put("package", packageName);
    model.put("groupId", packageName);
    model.put("artifactId", name);
    model.put("version", "1.0");
    model.put("joobyVersion", new VersionProvider().getVersion()[0]);
    model.put("server", "netty");
    model.put("kotlin", kotlin);

    withWriter(projectDir.resolve(buildFileName),
        writer -> ctx.templates.compile(templateName).apply(model, writer));

    // Copy conf
    Path confDir = projectDir.resolve("conf");
    Files.createDirectory(confDir);
    withInputStream("/conf/application.conf",
        in -> Files.copy(in, confDir.resolve("application.conf")));
    withInputStream("/conf/logback.xml",
        in -> Files.copy(in, confDir.resolve("logback.xml")));

    /** Source directories: */
    Path sourcePath = projectDir.resolve("src").resolve("main");
    Path javaPath = sourcePath.resolve(language);
    Path packagePath = Stream.of(packageName.split("\\."))
        .reduce(javaPath, Path::resolve, Path::resolve);

    Files.createDirectories(packagePath);
    withWriter(packagePath.resolve("App." + extension),
        writer -> ctx.templates.compile("App." + extension).apply(model, writer));

    /** Test directories: */
    Path testPath = projectDir.resolve("src").resolve("test");
    Path testJavaPath = testPath.resolve(language);
    Path testPackagePath = Stream.of(packageName.split("\\."))
        .reduce(testJavaPath, Path::resolve, Path::resolve);
    Files.createDirectories(testPackagePath);
    withWriter(testPackagePath.resolve("UnitTest." + extension),
        writer -> ctx.templates.compile("UnitTest." + extension).apply(model, writer));
    withWriter(testPackagePath.resolve("IntegrationTest." + extension),
        writer -> ctx.templates.compile("IntegrationTest." + extension).apply(model, writer));
  }

  private void withInputStream(String resource, SneakyThrows.Consumer<InputStream> consumer)
      throws IOException {
    try (InputStream in = getClass().getResourceAsStream(resource)) {
      consumer.accept(in);
    }
  }

  private void withWriter(Path file, SneakyThrows.Consumer<PrintWriter> consumer)
      throws IOException {
    try (PrintWriter writer = new PrintWriter(file.toFile(), "UTF-8")) {
      consumer.accept(writer);
      writer.flush();
    }
  }
}
