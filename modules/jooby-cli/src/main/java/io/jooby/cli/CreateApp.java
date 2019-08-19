/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.cli;

import picocli.CommandLine;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@CommandLine.Command(name = "create", description = "Creates a new application")
public class CreateApp extends Command implements Runnable {
  @CommandLine.Parameters
  private String name;

  @CommandLine.Option(names = {"-g", "--gradle"})
  private boolean gradle;

  @CommandLine.Option(names = {"-kt", "--kotlin"}, description = "Generate a Kotlin application")
  private boolean kotlin;

  @CommandLine.Option(names = {"-X"}, description = "Print debug information")
  private boolean debug;

  @Override public void run(CommandContext ctx) throws Exception {
    if (debug) {
      debug(ctx);
    }
    Path projectDir = Paths.get(System.getProperty("user.dir"), name);
    if (Files.exists(projectDir)) {
      throw new IOException("Project directory already exists: " + projectDir);
    }
    Files.createDirectory(projectDir);
    String templateName = gradle ? "build.gradle" : "maven";
    String buildFileName = gradle ? "build.gradle" : "pom.xml";
    String packageName = "app";
    String language = kotlin ? "kotlin" : "java";
    String extension = kotlin ? "kt" : "java";

    Map<String, Object> model = new HashMap<>();
    model.put("package", packageName);
    model.put("groupId", packageName);
    model.put("artifactId", name);
    model.put("version", "1.0");
    model.put("joobyVersion", new VersionProvider().getVersion()[0]);
    model.put("server", "netty");
    model.put("kotlin", kotlin);
    model.put("dependencies", dependencies("netty", kotlin));
    model.put("testDependencies", testDependencies(kotlin));

    ctx.writeTemplate(templateName, model, projectDir.resolve(buildFileName));

    // Copy conf
    Path confDir = projectDir.resolve("conf");
    copyResource("/cli/conf/application.conf", confDir.resolve("application.conf"));
    copyResource("/cli/conf/logback.xml", confDir.resolve("logback.xml"));

    if (gradle) {
      gradleWrapper(ctx, projectDir, model);
    }

    /** Source directories: */
    Path sourcePath = projectDir.resolve("src").resolve("main");
    Path javaPath = sourcePath.resolve(language);
    Path packagePath = Stream.of(packageName.split("\\."))
        .reduce(javaPath, Path::resolve, Path::resolve);

    ctx.writeTemplate("App." + extension, model, packagePath.resolve("App." + extension));

    /** Test directories: */
    Path testPath = projectDir.resolve("src").resolve("test");
    Path testJavaPath = testPath.resolve(language);
    Path testPackagePath = Stream.of(packageName.split("\\."))
        .reduce(testJavaPath, Path::resolve, Path::resolve);

    ctx.writeTemplate("UnitTest." + extension, model,
        testPackagePath.resolve("UnitTest." + extension));
    ctx.writeTemplate("IntegrationTest." + extension, model,
        testPackagePath.resolve("IntegrationTest." + extension));
  }

  private void gradleWrapper(CommandContext ctx, Path projectDir, Map<String, Object> model)
      throws IOException {
    Path wrapperDir = projectDir.resolve("gradle").resolve("wrapper");
    Files.createDirectories(wrapperDir);

    ctx.writeTemplate("gradle/settings.gradle", model, projectDir.resolve("settings.gradle"));
    copyResource("/cli/gradle/gradlew", projectDir.resolve("gradlew"),
        EnumSet.allOf(PosixFilePermission.class));

    copyResource("/cli/gradle/gradlew.bat", projectDir.resolve("gradlew.bat"));

    copyResource("/cli/gradle/gradle/wrapper/gradle-wrapper.jar",
        wrapperDir.resolve("gradle-wrapper.jar"));
    copyResource("/cli/gradle/gradle/wrapper/gradle-wrapper.properties",
        wrapperDir.resolve("gradle-wrapper.properties"));
  }

  private void debug(CommandContext ctx) {
    StringBuilder buffer = new StringBuilder();
    buffer.append("name: ").append(name);
    if (gradle) {
      buffer.append(" --gradle");
    } else {
      buffer.append(" --maven");
    }
    if (kotlin) {
      buffer.append(" --kotlin");
    } else {
      buffer.append(" --java");
    }
    ctx.out.println(buffer.toString());
  }

  private void copyResource(String source, Path dest) throws IOException {
    copyResource(source, dest, Collections.emptySet());
  }

  private void copyResource(String source, Path dest, Set<PosixFilePermission> permissions)
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

  private List<Dependency> dependencies(String server, boolean kotlin) {
    List<Dependency> dependencies = new ArrayList<>();
    dependencies.add(new Dependency("io.jooby", "jooby-" + server));
    if (kotlin) {
      dependencies.add(new Dependency("org.jetbrains.kotlin", "kotlin-stdlib-jdk8"));
    }
    dependencies.add(new Dependency("ch.qos.logback", "logback-classic"));
    return dependencies;
  }

  private List<Dependency> testDependencies(boolean kotlin) {
    List<Dependency> dependencies = new ArrayList<>();
    dependencies.add(new Dependency("org.junit.jupiter", "junit-jupiter-api"));
    dependencies.add(new Dependency("org.junit.jupiter", "junit-jupiter-engine"));
    dependencies.add(new Dependency("io.jooby", "jooby-test"));
    dependencies.add(new Dependency("com.squareup.okhttp3", "okhttp"));
    return dependencies;
  }
}
