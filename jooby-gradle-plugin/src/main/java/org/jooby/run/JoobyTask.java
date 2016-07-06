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
package org.jooby.run;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.gradle.api.Project;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.TaskAction;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;

public class JoobyTask extends ConventionTask {

  private static final Object LOCK = new Object();

  private static final List<String> XML_PROPS = Arrays.asList(
      "javax.xml.parsers.DocumentBuilderFactory",
      "javax.xml.parsers.SAXParserFactory",
      "javax.xml.stream.XMLInputFactory",
      "javax.xml.stream.XMLEventFactory",
      "javax.xml.transform.TransformerFactory",
      "javax.xml.stream.XMLOutputFactory",
      "javax.xml.datatype.DatatypeFactory",
      "org.xml.sax.driver");

  private List<String> includes;

  private List<String> excludes;

  private String logLevel;

  private boolean block;

  private Set<File> classpath;

  private Set<File> src;

  private String mainClassName;

  private String compiler;

  @TaskAction
  public void run() throws Exception {
    System.setProperty("logLevel", getLogLevel());

    Project project = getProject();

    String mId = project.getName();

    List<File> cp = new ArrayList<>();

    // conf & public
    getClasspath().forEach(cp::add);

    Main app = new Main(mId, getMainClassName(), cp.toArray(new File[cp.size()]));
    if (includes != null) {
      app.includes(includes.stream().collect(Collectors.joining(File.pathSeparator)));
    }

    if (excludes != null) {
      app.excludes(excludes.stream().collect(Collectors.joining(File.pathSeparator)));
    }

    String compiler = getCompiler();
    getLogger().info("compiler is {}", compiler);
    if ("on".equalsIgnoreCase(compiler)) {
      Path[] watchDirs = getSrc().stream()
          .map(File::toPath)
          .collect(Collectors.toList())
          .toArray(new Path[0]);
      // don't start watcher if continuous is ON
      new Watcher((k, path) -> {
        if (path.toString().endsWith(".java")) {
          runTask(project, path, "classes");
        } else if (path.toString().endsWith(".conf")
            || path.toString().endsWith(".properties")) {
          runTask(project, path, "classes");
        }
      }, watchDirs).start();
    }

    String[] args = project.getGradle().getStartParameter().getProjectProperties()
        .entrySet().stream().map(e -> e.toString()).collect(Collectors.toList())
        .toArray(new String[0]);
    app.run(isBlock(), args);
  }

  private void runTask(final Project project, final Path path, final String task) {
    synchronized (LOCK) {
      ProjectConnection connection = null;
      Map<String, String> xml = new HashMap<>();
      try {
        // clean jaxp
        XML_PROPS.forEach(p -> xml.put(p, (String) System.getProperties().remove(p)));

        connection = GradleConnector.newConnector()
            .useInstallation(project.getGradle().getGradleHomeDir())
            .forProjectDirectory(project.getRootDir())
            .connect();

        try {
          connection.newBuild()
              .setStandardError(System.err)
              .setStandardOutput(System.out)
              .forTasks(task)
              .run();
        } catch (Exception ex) {
          getLogger().debug("Execution of " + task + " resulted in exception", ex);
        }

      } finally {
        // restore jaxp
        xml.forEach((k, v) -> {
          if (v != null) {
            System.setProperty(k, v);
          }
        });
        if (connection != null) {
          connection.close();
        }
      }
    }
  }

  public void setIncludes(final List<String> includes) {
    this.includes = includes;
  }

  public List<String> getIncludes() {
    return includes;
  }

  public void setExcludes(final List<String> excludes) {
    this.excludes = excludes;
  }

  public List<String> getExcludes() {
    return excludes;
  }

  public void setLogLevel(final String logLevel) {
    this.logLevel = logLevel;
  }

  public String getLogLevel() {
    return logLevel;
  }

  @InputFiles
  public Set<File> getClasspath() {
    return classpath;
  }

  public void setClasspath(final Set<File> classpath) {
    this.classpath = classpath;
  }

  public void setBlock(final boolean block) {
    this.block = block;
  }

  public boolean isBlock() {
    return block;
  }

  public String getMainClassName() {
    return mainClassName;
  }

  public void setMainClassName(final String mainClassName) {
    this.mainClassName = mainClassName;
  }

  public Set<File> getSrc() {
    return src;
  }

  public void setSrc(final Set<File> watchDirs) {
    this.src = watchDirs;
  }

  public String getCompiler() {
    return compiler;
  }

  public void setCompiler(final String compiler) {
    this.compiler = compiler;
  }
}
