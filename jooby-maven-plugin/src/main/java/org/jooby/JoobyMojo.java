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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
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

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    Set<String> classpath = new LinkedHashSet<String>();

    // public / config, etc..
    classpath.addAll(resources(mavenProject.getResources()));

    // target/classes
    classpath.add(buildOutputDirectory);

    // *.jar
    for (Object candidate : mavenProject.getArtifacts()) {
      Artifact artifact = (Artifact) candidate;
      classpath.add(artifact.getFile().toString());
    }
    String cp = classpath.stream().collect(Collectors.joining(File.pathSeparator));

    // prepare commands
    List<Command> cmds = new ArrayList<Command>();
    if (commands != null && commands.size() > 0) {
      cmds.addAll(0, this.commands);
    }
    List<String> args = new ArrayList<String>();
    args.addAll(vmArgs(vmArgs));
    args.add("-cp");
    args.add(cp);
    args.add(mainClass);
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

  private List<String> vmArgs(final List<String> vmArgs) {
    List<String> results = new ArrayList<String>();
    if (vmArgs != null) {
      results.addAll(vmArgs);
    }
    File[] logbackFiles = {localFile("config", "logback-test.xml"),
        localFile("config", "logback.xml") };
    for (File logback : logbackFiles) {
      if (logback.exists()) {
        results.add("-Dlogback.configurationFile=" + logback.getAbsolutePath());
        break;
      }
    }
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
}
