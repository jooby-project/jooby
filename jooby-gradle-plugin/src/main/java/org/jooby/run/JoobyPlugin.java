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
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.internal.ConventionMapping;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.plugins.Convention;

public class JoobyPlugin implements Plugin<Project> {

  static {
    // Gradle hack: OS j2v8 dependency
    String family = os(System.getProperty("os.name", "").toLowerCase());
    String arch = osarch(System.getProperty("os.arch", "").toLowerCase());
    Object j2v8 = "j2v8_" + family + "_" + arch;
    System.getProperties().put("j2v8", j2v8);
  }

  @Override
  public void apply(final Project project) {
    Map<String, Object> options = new HashMap<>();
    Convention convention = project.getConvention();
    convention.getPlugins().put("joobyRun", options);

    try {
      configureJoobyRun(project, options);
    } catch (IOException ex) {
      throw new IllegalStateException("Unable to configure joobyRun", ex);
    }

    configureJoobyAssets(project, options);

  }

  private static String os(final String os) {
    if (os.contains("windows")) {
      return "win32";
    } else if (os.contains("linux")) {

      return "linux";
    } else if (os.contains("mac")) {
      return "macosx";
    }
    return os;
  }

  private static String osarch(final String arch) {
    if (arch.contains("x86_64")) {
      return "x86_64";
    } else if (arch.contains("x86")) {
      return "x86";
    } else if (arch.contains("amd64")) {
      return "amd64";
    }
    return arch;
  }

  private void configureJoobyRun(final Project project, final Map<String, Object> conf)
      throws IOException {
    project.getTasks()
        .withType(JoobyTask.class, joobyRun -> {
          ConventionMapping mapping = joobyRun.getConventionMapping();

          mapping.map("classpath", () -> new JoobyProject(project).classpath());

          mapping.map("src", () -> new JoobyProject(project).sources());

          mapping.map("mainClassName", () -> project.getProperties().get("mainClassName"));

          Gradle gradle = project.getGradle();
          mapping.map("block", () -> !gradle.getStartParameter().isContinuous());
          mapping.map("logLevel", () -> gradle.getStartParameter().getLogLevel().name());
        });

    Map<String, Object> options = new HashMap<>();
    options.put(Task.TASK_TYPE, JoobyTask.class);
    options.put(Task.TASK_DEPENDS_ON, "classes");
    options.put(Task.TASK_NAME, "joobyRun");
    options.put(Task.TASK_DESCRIPTION, "Run, debug and hot reload applications");
    options.put(Task.TASK_GROUP, "jooby");
    project.getTasks().create(options);
  }

  private void configureJoobyAssets(final Project project, final Map<String, Object> conf) {
    project.getTasks()
        .withType(AssetTask.class, task -> {
          ConventionMapping mapping = task.getConventionMapping();

          mapping.map("env", () -> "dist");

          mapping.map("maxAge", () -> "365d");

          mapping.map("mainClassName", () -> project.getProperties().get("mainClassName"));

          mapping.map("output", () -> new JoobyProject(project).buildResources());

          mapping.map("assemblyOutput", () -> new File(project.getBuildDir(), "__public_"));
        });

    Map<String, Object> options = new HashMap<>();
    options.put(Task.TASK_TYPE, AssetTask.class);
    options.put(Task.TASK_DEPENDS_ON, "classes");
    options.put(Task.TASK_NAME, "joobyAssets");
    options.put(Task.TASK_DESCRIPTION, "Process, optimize and compress static files");
    options.put(Task.TASK_GROUP, "jooby");
    project.getTasks().create(options);
  }
}
