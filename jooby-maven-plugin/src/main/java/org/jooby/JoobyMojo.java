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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
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

import com.google.common.io.Files;

@Mojo(name = "run", threadSafe = true, requiresDependencyResolution = ResolutionScope.TEST)
@Execute(phase = LifecyclePhase.TEST_COMPILE)
public class JoobyMojo extends AbstractMojo {

  @Component
  private MavenProject mavenProject;

  @Parameter(property = "main.class", defaultValue = "${application.class}")
  private String mainClass;

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

  @SuppressWarnings("unchecked")
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {

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
    args.addAll(appcp);
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
    /**
     * Shutdown hook
     */
    Runtime.getRuntime().addShutdownHook(shutdownHook(cmds, getLog()));

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

  private void dumpSysProps(final Path path) throws MojoFailureException {
    try {
      FileOutputStream output = new FileOutputStream(path.toFile());
      Properties properties = System.getProperties();
      properties.store(output, "system properties");
    } catch (IOException ex) {
      throw new MojoFailureException("Can't dump system properties to: " + path, ex);
    }
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
      Path moddir = jmodules;
      for (String p : project.getGroupId().split("\\.")) {
        moddir = moddir.resolve(p);
      }
      for (String p : project.getArtifactId().split("\\.")) {
        moddir = moddir.resolve(p);
      }
      moddir = moddir.resolve("main");

      // resources
      StringBuilder rsb = new StringBuilder();
      for (String resource : resources) {
        Path resourceRoot = new File(resource).toPath();
        rsb.append("    <resource-root path=\"").append(moddir.relativize(resourceRoot))
            .append("\" />\n");
      }
      // maven dependencies
      for (Artifact artifact : artifacts) {
        // not pom
        if (!"pom".equals(artifact.getType())) {
          rsb.append("    <artifact name=\"").append(artifact.getGroupId()).append(":")
              .append(artifact.getArtifactId()).append(":").append(artifact.getVersion())
              .append("\" />\n");
        }
      }

      String content = jbossModule(project.getGroupId(), project.getArtifactId(), rsb, null);
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

  private List<String> resources(final Iterable<Resource> resources) {
    List<String> result = new ArrayList<String>();
    for (Resource resource : resources) {
      String dir = resource.getDirectory();
      if (new File(dir).exists()) {
        result.add(dir);
      }
    }
    return result;
  }

  private static Thread shutdownHook(final List<Command> cmds, final Log log) {
    return new Thread() {
      @Override
      public void run() {
        for (Command command : cmds) {
          try {
            log.info("stopping: " + command);
            command.stop();
          } catch (Exception ex) {
            log.error("Stopping process: " + command + " resulted in error", ex);
          }
        }
      }
    };
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
