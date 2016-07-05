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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.internal.ConventionMapping;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.plugins.Convention;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;

public class JoobyPlugin implements Plugin<Project> {

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
  }

  private void configureJoobyRun(final Project project, final Map<String, Object> conf)
      throws IOException {

    project.getTasks()
        .withType(JoobyTask.class, joobyRun -> {
          ConventionMapping mapping = joobyRun.getConventionMapping();

          mapping.map("classpath", () -> classpath(project));

          mapping.map("watchDirs", () -> watchDirs(project));

          mapping.map("mainClassName", () -> project.getProperties().get("mainClassName"));

          Gradle gradle = project.getGradle();
          mapping.map("block", () -> !gradle.getStartParameter().isContinuous());
          mapping.map("logLevel", () -> gradle.getStartParameter().getLogLevel().name());
        });

    Map<String, Object> options = new HashMap<>();
    options.put(Task.TASK_TYPE, JoobyTask.class);
    options.put(Task.TASK_DEPENDS_ON, "classes");
    options.put(Task.TASK_NAME, "joobyRun");
    options.put(Task.TASK_GROUP, "jooby");
    project.getTasks().create(options);
  }

  private Set<File> classpath(final Project project) {
    SourceSet sourceSet = sourceSet(project);

    Set<File> cp = new LinkedHashSet<>();
    // conf & public
    sourceSet.getResources().getSrcDirs().forEach(cp::add);
    // classes/main, resources/main + jars
    sourceSet.getRuntimeClasspath().getFiles().forEach(cp::add);
    return cp;
  }

  private Set<File> watchDirs(final Project project) {
    SourceSet sourceSet = sourceSet(project);

    Set<File> cp = new LinkedHashSet<>();
    // conf & public
    sourceSet.getResources().getSrcDirs().forEach(cp::add);
    // source java
    sourceSet.getJava().getSrcDirs().forEach(cp::add);
    return cp;
  }

  private SourceSet sourceSet(final Project project) {
    SourceSet sourceSet = getJavaConvention(project).getSourceSets()
        .getByName(SourceSet.MAIN_SOURCE_SET_NAME);
    return sourceSet;
  }

  public JavaPluginConvention getJavaConvention(final Project project) {
    return project.getConvention().getPlugin(JavaPluginConvention.class);
  }
}
