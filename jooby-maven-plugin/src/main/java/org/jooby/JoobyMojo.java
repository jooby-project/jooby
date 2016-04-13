/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.maven.Maven;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.jooby.hotreload.Watcher;

import com.google.common.io.Files;

import javaslang.control.Try;

@Mojo(name = "run", threadSafe = true, requiresDependencyResolution = ResolutionScope.TEST)
@Execute(phase = LifecyclePhase.TEST_COMPILE)
public class JoobyMojo extends AbstractMojo {

  private static class ShutdownHook extends Thread {
    private Log log;

    private List<Command> commands;

    private Watcher watcher;

    public ShutdownHook(final Log log, final List<Command> commands) {
      this.log = log;
      this.commands = commands;
      setDaemon(true);
    }

    @Override
    public void run() {
      if (watcher != null) {
        log.info("stopping: watcher");
        Try.run(watcher::stop).onFailure(ex -> log.debug("Stop of watcher resulted in error", ex));
      }
      commands.forEach(cmd -> {
        log.info("stopping: " + cmd);
        Try.run(cmd::stop).onFailure(ex -> log.error("Stop of " + cmd + " resulted in error", ex));
      });
    }
  }

  @Component
  private MavenProject mavenProject;

  @Parameter(defaultValue = "${session}", required = true, readonly = true)
  protected MavenSession session;

  @Parameter(property = "main.class", defaultValue = "${application.class}")
  protected String mainClass;

  @Parameter(defaultValue = "${project.build.outputDirectory}")
  private String buildOutputDirectory;

  @Parameter(property = "jooby.commands")
  private List<Command> commands;

  @Parameter(property = "jooby.vmArgs")
  private List<String> vmArgs;

  @Parameter(property = "jooby.includes")
  private List<String> includes;

  @Parameter(property = "jooby.excludes")
  private List<String> excludes;

  @Parameter(property = "application.debug", defaultValue = "true")
  private String debug;

  @Parameter(defaultValue = "${plugin.artifacts}")
  private List<org.apache.maven.artifact.Artifact> pluginArtifacts;

  @Parameter(property = "compiler", defaultValue = "on")
  private String compiler;

  @Component
  protected Maven maven;

  @SuppressWarnings("unchecked")
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {

    boolean js = new File("app.js").exists();
    if (js) {
      mainClass = "org.jooby.Jooby";
    }
    Path jmodules = Paths.get(mavenProject.getBuild().getDirectory()).resolve("jboss-modules");

    Set<String> appcp = new LinkedHashSet<String>();

    // public / config, etc..
    appcp.addAll(resources(mavenProject.getResources()));

    // target/classes
    appcp.add(buildOutputDirectory);

    // *.jar
    Set<Artifact> artifacts = new LinkedHashSet<Artifact>(mavenProject.getArtifacts());

    doFlatMainModule(mavenProject, jmodules, appcp, artifacts);

    // allow to access command line system properties
    dumpSysProps(jmodules.resolve("sys.properties"));

    Set<String> classpath = new LinkedHashSet<String>();

    String hotreload = extra(pluginArtifacts, "jooby-hotreload").get().getFile().getAbsolutePath();
    String jbossModules = extra(pluginArtifacts, "jboss-modules").get().getFile().getAbsolutePath();
    classpath.add(hotreload);
    classpath.add(jbossModules);

    String cp = classpath.stream().collect(Collectors.joining(File.pathSeparator));

    // prepare commands
    List<Command> cmds = new ArrayList<Command>();
    if (commands != null && commands.size() > 0) {
      cmds.addAll(0, this.commands);
    }
    List<String> args = new ArrayList<String>();
    args.addAll(vmArgs(hotreload, vmArgs));
    args.add("-cp");
    args.add(cp);
    args.add("org.jooby.hotreload.AppModule");
    args.add(mavenProject.getGroupId() + "." + mavenProject.getArtifactId());
    args.add(mainClass);
    args.add(jmodules.toString());
    args.add(mavenProject.getBasedir().getAbsolutePath());
    if (includes != null && includes.size() > 0) {
      args.add("includes=" + join(includes));
    }
    if (excludes != null && excludes.size() > 0) {
      args.add("excludes=" + join(excludes));
    }

    cmds.add(new Command(mainClass, "java", args));

    for (Command cmd : cmds) {
      cmd.setWorkdir(mavenProject.getBasedir());
      getLog().debug("cmd: " + cmd.debug());
    }

    Watcher watcher = setupCompiler(mavenProject, compiler, goal -> {
      maven.execute(DefaultMavenExecutionRequest.copy(session.getRequest())
          .setGoals(Arrays.asList(goal)));

    });
    ShutdownHook shutdownHook = new ShutdownHook(getLog(), cmds);
    shutdownHook.watcher = watcher;
    /**
     * Shutdown hook
     */
    Runtime.getRuntime().addShutdownHook(shutdownHook);

    if (watcher != null) {
      watcher.start();
    }

    /**
     * Start process
     */
    for (Command cmd : cmds) {
      try {
        getLog().debug("Starting process: " + cmd);
        cmd.execute();
      } catch (Exception ex) {
        throw new MojoFailureException("Execution of " + cmd + " resulted in error", ex);
      }
    }

  }

  @SuppressWarnings("unchecked")
  private static Watcher setupCompiler(final MavenProject project, final String compiler,
      final Consumer<String> task) throws MojoFailureException {
    File eclipseClasspath = new File(project.getBasedir(), ".classpath");
    if ("off".equalsIgnoreCase(compiler) || eclipseClasspath.exists()) {
      return null;
    }
    List<String> resources = resources(project.getResources());
    resources.add(0, project.getBuild().getSourceDirectory());
    Path[] paths = new Path[resources.size()];
    for (int i = 0; i < paths.length; i++) {
      paths[i] = Paths.get(resources.get(i));
    }
    try {
      return new Watcher((kind, path) -> {
        if (path.toString().endsWith(".java")) {
          task.accept("compile");
        } else if (path.toString().endsWith(".conf")
            || path.toString().endsWith(".properties")) {
          task.accept("compile");
        }
      }, paths);
    } catch (Exception ex) {
      throw new MojoFailureException("Can't compile source code", ex);
    }
  }

  private void dumpSysProps(final Path path) throws MojoFailureException {
    try {
      FileOutputStream output = new FileOutputStream(path.toFile());
      Properties properties = System.getProperties();
      properties.setProperty("application.version", mavenProject.getVersion());
      properties.store(output, "system properties");
    } catch (IOException ex) {
      throw new MojoFailureException("Can't dump system properties to: " + path, ex);
    }
  }

  private Path mpath(final String value) {
    Path path = null;
    for (String p : value.split("\\.")) {
      if (path == null) {
        path = Paths.get(p);
      } else {
        path = path.resolve(p);
      }
    }
    return path;
  }

  /**
   * Creates a module.
   *
   * @param project
   * @param jmodules
   * @param resources
   * @param artifacts
   * @throws MojoFailureException
   */
  private void doFlatMainModule(final MavenProject project, final Path jmodules,
      final Set<String> resources, final Set<Artifact> artifacts) throws MojoFailureException {
    try {
      Path moddir = jmodules.resolve(mpath(project.getGroupId()))
          .resolve(mpath(project.getArtifactId())).resolve("main");

      // resources
      StringBuilder rsb = new StringBuilder();
      for (String resource : resources) {
        Path resourceRoot = new File(resource).toPath();
        rsb.append("    <resource-root path=\"").append(moddir.relativize(resourceRoot))
            .append("\" />\n");
      }
      StringBuilder dsb = new StringBuilder();
      // maven dependencies
      for (Artifact artifact : artifacts) {
        // not pom
        if (!"pom".equals(artifact.getType())) {
          if (artifact.getGroupId().equals("com.eclipsesource.j2v8")) {
            dsb.append("    <module name=\"").append(artifact.getGroupId()).append(".")
                .append(artifact.getArtifactId()).append("\" />\n");

            StringBuilder arsb = new StringBuilder();

            arsb.append("    <artifact name=\"").append(artifact.getGroupId()).append(":")
                .append(artifact.getArtifactId()).append(":").append(artifact.getVersion());

            String classifier = artifact.getClassifier();
            if (classifier != null && classifier.length() > 0) {
              arsb.append(":").append(classifier);
            }
            arsb.append("\" />\n");

            String content = jbossModule(artifact.getGroupId(), artifact.getArtifactId(), arsb,
                null);
            Path artdir = jmodules.resolve(mpath(artifact.getGroupId()))
                .resolve(mpath(artifact.getArtifactId())).resolve("main");
            artdir.toFile().mkdirs();
            Files.write(content, artdir.resolve("module.xml").toFile(), StandardCharsets.UTF_8);

          } else {
            rsb.append("    <artifact name=\"").append(artifact.getGroupId()).append(":")
                .append(artifact.getArtifactId()).append(":").append(artifact.getVersion());
            String classifier = artifact.getClassifier();
            if (classifier != null && classifier.length() > 0) {
              rsb.append(":").append(classifier);
            }
            rsb.append("\" />\n");
          }
        }
      }

      String content = jbossModule(project.getGroupId(), project.getArtifactId(), rsb, dsb);
      moddir.toFile().mkdirs();
      Files.write(content, moddir.resolve("module.xml").toFile(), StandardCharsets.UTF_8);
    } catch (Exception ex) {
      throw new MojoFailureException("Can't create repository", ex);
    }
  }

  private String jbossModule(final String groupId, final String artifactId,
      final StringBuilder resources, final StringBuilder deps) {
    StringBuilder sb = new StringBuilder();
    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    sb.append("<module xmlns=\"urn:jboss:module:1.3\" name=\"").append(groupId).append(".")
        .append(artifactId).append("\">\n");
    if (resources != null) {
      sb.append("  <resources>\n");
      sb.append(resources);
      sb.append("  </resources>\n");
    }
    if (deps != null) {
      sb.append("  <dependencies>\n");
      sb.append(deps);
      sb.append("  </dependencies>\n");
    }
    sb.append("</module>\n");
    return sb.toString();
  }

  private String join(final List<String> includes) {
    StringBuilder buff = new StringBuilder();
    for (String include : includes) {
      buff.append(include).append(":");
    }
    return buff.toString();
  }

  private List<String> vmArgs(final String agentpath, final List<String> vmArgs) {
    List<String> results = new ArrayList<String>();
    if (vmArgs != null) {
      results.addAll(vmArgs);
    }
    if (!"false".equals(debug)) {
      // true, number, debug line
      if ("true".equals(debug)) {
        // default debug
        results.add("-agentlib:jdwp=transport=dt_socket,address=8000,server=y,suspend=n");
      } else {
        try {
          int port = Integer.parseInt(debug);
          results.add("-agentlib:jdwp=transport=dt_socket,address=" + port + ",server=y,suspend=n");
        } catch (NumberFormatException ex) {
          // assume it is a debug line
          results.add(debug);
        }
      }
    }
    // logback
    File[] logbackFiles = {localFile("conf", "logback-test.xml"),
        localFile("conf", "logback.xml") };
    for (File logback : logbackFiles) {
      if (logback.exists()) {
        results.add("-Dlogback.configurationFile=" + logback.getAbsolutePath());
        break;
      }
    }
    // dcevm? OFF
    // String altjvm = null;
    // for (String boot : System.getProperty("sun.boot.library.path", "").split(File.pathSeparator))
    // {
    // File dcevm = new File(boot, "dcevm");
    // if (dcevm.exists()) {
    // altjvm = dcevm.getName();
    // }
    // }
    // if (altjvm == null) {
    // getLog().error("dcevm not found, please install it: https://github.com/dcevm/dcevm");
    // } else {
    // results.add("-XXaltjvm=" + altjvm);
    // }
    return results;
  }

  private File localFile(final String... paths) {
    File result = mavenProject.getBasedir();
    for (String path : paths) {
      result = new File(result, path);
    }
    return result;
  }

  private static List<String> resources(final Iterable<Resource> resources) {
    List<String> result = new ArrayList<String>();
    for (Resource resource : resources) {
      String dir = resource.getDirectory();
      if (new File(dir).exists()) {
        result.add(dir);
      }
    }
    return result;
  }

  private Optional<Artifact> extra(final List<Artifact> artifacts, final String name) {
    for (Artifact artifact : artifacts) {
      for (String tail : artifact.getDependencyTrail()) {
        if (tail.contains(name)) {
          return Optional.of(artifact);
        }
      }
    }
    return Optional.empty();
  }

}
