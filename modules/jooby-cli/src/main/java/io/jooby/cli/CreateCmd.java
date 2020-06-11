/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.cli;

import io.jooby.internal.cli.Dependency;
import picocli.CommandLine;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Create application command.
 *
 * Usage:
 * <pre>{@code
 * jooby> help create
 * Missing required parameter: <name>
 * Usage: jooby create [-dgi] [-kt] [-stork] [-s=<server>] <name>
 * Creates a new application
 *       <name>              Application name or coordinates (groupId:artifactId:
 *                             version)
 *   -d, --docker            Generates a dockerfile
 *   -g, --gradle            Generates a Gradle project
 *   -i                      Start interactive mode
 *       -kt, --kotlin       Generates a Kotlin application
 *   -s, --server=<server>   Choose one of the available servers: jetty, netty or
 *                             undertow
 *       -stork              Add Stork Maven plugin to build (Maven only)
 * }</pre>
 *
 * @since 2.0.6
 */
@CommandLine.Command(name = "create", description = "Creates a new application")
public class CreateCmd extends Cmd {
  @CommandLine.Parameters(
      description = "Application name or coordinates (groupId:artifactId:version)"
  )
  private String name;

  @CommandLine.Option(
      names = {"-g", "--gradle"},
      description = "Generates a Gradle project"
  )
  private boolean gradle;

  @CommandLine.Option(
      names = {"-k", "--kotlin"},
      description = "Generates a Kotlin application"
  )
  private boolean kotlin;

  @CommandLine.Option(
      names = {"--stork"},
      description = "Add Stork Maven plugin to build (Maven only)"
  )
  private boolean stork;

  @CommandLine.Option(
      names = {"-i"},
      description = "Start interactive mode"
  )
  private boolean interactive;

  @CommandLine.Option(
      names = {"--server"},
      description = "Choose one of the available servers: jetty, netty or undertow"
  )
  private String server;

  @CommandLine.Option(
      names = {"--docker"},
      description = "Generates a Dockerfile"
  )
  private boolean docker;

  @CommandLine.Option(
      names = {"-m", "--mvc"},
      description = "Generates a MVC application"
  )
  private boolean mvc;

  @CommandLine.Option(
      names = {"--openapi"},
      description = "Configure build to generate OpenAPI files"
  )
  private boolean openapi;

  @Override public void run(@Nonnull Context ctx) throws Exception {
    Path projectDir = ctx.getWorkspace().resolve(name);
    if (Files.exists(projectDir)) {
      throw new IOException("Project directory already exists: " + projectDir);
    }
    Files.createDirectory(projectDir);
    String packageName;
    String version;
    String server;
    boolean stork = !gradle && this.stork;
    if (interactive) {
      gradle = yesNo(ctx.readLine("Use Gradle (yes/No): "));

      kotlin = yesNo(ctx.readLine("Use Kotlin (yes/No): "));

      packageName = ctx.readLine("Enter a groupId/package: ");

      version = ctx.readLine("Enter a version (1.0.0): ");
      if (version == null || version.trim().length() == 0) {
        version = "1.0.0";
      }

      mvc = yesNo(ctx.readLine("Use MVC (yes/No): "));

      openapi = yesNo(ctx.readLine("Configure OpenAPI (yes/No): "));

      server = server(ctx.readLine("Choose a server (jetty, netty or undertow): "));

      if (!gradle) {
        stork = distribution(ctx.readLine("Distribution (uber/fat jar or stork): "))
            .equals("stork");
      }

      docker = yesNo(ctx.readLine("Generates Dockerfile (yes/No): "));
    } else {
      String[] parts = name.split(":");
      switch (parts.length) {
        case 3:
          packageName = parts[0];
          name = parts[1];
          version = parts[2];
          break;
        case 2:
          packageName = parts[0];
          name = parts[1];
          version = "1.0.0";
          break;
        default:
          packageName = "app";
          version = "1.0.0";
      }
      server = server(this.server);
    }
    String templateName = gradle ? "build.gradle" : "pom.xml";
    String buildFileName = templateName;
    String language = kotlin ? "kotlin" : "java";
    String extension = language.equalsIgnoreCase("kotlin") ? "kt" : "java";

    String finalArtifactId;
    if (gradle) {
      finalArtifactId = Paths.get("build", "libs", name + "-" + version + "-all.jar")
          .toString();
    } else {
      finalArtifactId = name + "-" + version + (stork ? ".zip" : ".jar");
    }

    Map<String, Object> model = new HashMap<>();
    model.putAll(ctx.getDependencyMap());

    model.put("package", packageName);
    model.put("groupId", packageName);
    model.put("artifactId", name);
    model.put("version", version);
    model.put("joobyVersion", ctx.getVersion());
    model.put("server", server);
    model.put("kotlin", kotlin);
    model.put("dependencies", dependencies(server, kotlin));
    model.put("testDependencies", testDependencies(kotlin));
    model.put("stork", stork);
    model.put("gradle", gradle);
    model.put("maven", !gradle);
    model.put("docker", docker);
    model.put("mvc", mvc);
    model.put("openapi", openapi);
    model.put("kapt", mvc && kotlin);
    model.put("apt", mvc && !kotlin);
    model.put("finalArtifactId", finalArtifactId);

    ctx.writeTemplate(templateName, model, projectDir.resolve(buildFileName));

    // Copy conf
    Path confDir = projectDir.resolve("conf");
    ctx.copyResource("/cli/conf/application.conf", confDir.resolve("application.conf"));
    ctx.copyResource("/cli/conf/logback.xml", confDir.resolve("logback.xml"));

    ctx.writeTemplate("README.md", model, projectDir.resolve("README.md"));

    if (gradle) {
      gradleWrapper(ctx, projectDir, model);
    }

    if (stork) {
      stork(ctx, projectDir);
    }

    if (docker) {
      docker(ctx, projectDir, model);
    }

    /** Source directories: */
    Path sourcePath = projectDir.resolve("src").resolve("main");
    Path javaPath = sourcePath.resolve(language);
    Path packagePath = Stream.of(packageName.split("\\."))
        .reduce(javaPath, Path::resolve, Path::resolve);

    ctx.writeTemplate("App." + extension, model, packagePath.resolve("App." + extension));
    if (mvc) {
      ctx.writeTemplate("Controller." + extension, model,
          packagePath.resolve("Controller." + extension));
    }

    /** Test directories: */
    Path testPath = projectDir.resolve("src").resolve("test");
    Path testJavaPath = testPath.resolve(language);
    Path testPackagePath = Stream.of(packageName.split("\\."))
        .reduce(testJavaPath, Path::resolve, Path::resolve);

    ctx.writeTemplate("UnitTest." + extension, model,
        testPackagePath.resolve("UnitTest." + extension));
    ctx.writeTemplate("IntegrationTest." + extension, model,
        testPackagePath.resolve("IntegrationTest." + extension));

    ctx.println("Try it! Open a terminal and type: ");
    ctx.println("  cd " + projectDir.toAbsolutePath());
    ctx.println("  " + (gradle ? "./gradlew joobyRun" : "mvn jooby:run"));
  }

  private boolean yesNo(String value) {
    return "y".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value);
  }

  private void docker(Context ctx, Path dir, Map<String, Object> model) throws IOException {
    boolean gradle = (Boolean) model.get("gradle");
    String dockerfile = gradle ? "docker.gradle" : "docker.maven";
    ctx.writeTemplate(dockerfile, model, dir.resolve("Dockerfile"));
  }

  private Object distribution(String value) {
    if (value == null || value.trim().length() == 0) {
      return "uber";
    }
    switch (value.toLowerCase()) {
      case "fat":
      case "uber":
        return "uber";
      case "stork":
        return "stork";
      default:
        throw new IllegalArgumentException("Unknown distribution option: " + value);
    }
  }

  private String server(String value) {
    if (value == null || value.trim().length() == 0) {
      return "netty";
    }
    switch (value.toLowerCase()) {
      case "j":
      case "jetty":
        return "jetty";
      case "n":
      case "netty":
        return "netty";
      case "u":
      case "utow":
      case "undertow":
        return "utow";
      default:
        throw new IllegalArgumentException("Unknown server option: " + value);
    }
  }

  private void stork(Context ctx, Path projectDir) throws IOException {
    ctx.copyResource("/cli/src/etc/stork/stork.yml", projectDir.resolve("src").resolve("etc")
        .resolve("stork").resolve("stork.yml"));
  }

  private void gradleWrapper(Context ctx, Path projectDir, Map<String, Object> model)
      throws IOException {
    Path wrapperDir = projectDir.resolve("gradle").resolve("wrapper");

    ctx.writeTemplate("gradle/settings.gradle", model, projectDir.resolve("settings.gradle"));
    ctx.copyResource("/cli/gradle/gradlew", projectDir.resolve("gradlew"),
        EnumSet.allOf(PosixFilePermission.class));

    ctx.copyResource("/cli/gradle/gradlew.bat", projectDir.resolve("gradlew.bat"));

    ctx.copyResource("/cli/gradle/gradle/wrapper/gradle-wrapper.jar",
        wrapperDir.resolve("gradle-wrapper.jar"));
    ctx.copyResource("/cli/gradle/gradle/wrapper/gradle-wrapper.properties",
        wrapperDir.resolve("gradle-wrapper.properties"));
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
