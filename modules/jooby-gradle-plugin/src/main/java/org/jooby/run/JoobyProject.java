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
/**
 * This copy of Woodstox XML processor is licensed under the
 * Apache (Software) License, version 2.0 ("the License").
 * See the License for details about distribution rights, and the
 * specific rights regarding derivate works.
 *
 * You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/
 *
 * A copy is also included in the downloadable source code package
 * containing Woodstox, in file "ASL2.0", under the same directory
 * as this file.
 */
package org.jooby.run;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;

import javaslang.control.Try;

public class JoobyProject {

  private Project project;

  public JoobyProject(final Project project) {
    this.project = project;
  }

  public File buildResources() {
    SourceSet sourceSet = sourceSet(project);
    String resources = new File("resources", SourceSet.MAIN_SOURCE_SET_NAME).toString();
    return sourceSet.getRuntimeClasspath().getFiles().stream()
        .filter(f -> f.isDirectory() && f.toString().endsWith(resources)).findFirst().get();
  }

  public Set<File> classpath() {
    SourceSet sourceSet = sourceSet(project);

    Set<File> cp = new LinkedHashSet<>();
    // conf & public
    sourceSet.getResources().getSrcDirs().forEach(cp::add);

    // classes/main, resources/main + jars
    sourceSet.getRuntimeClasspath().getFiles().forEach(cp::add);

    // provided?
    Configuration provided = project.getConfigurations().findByName("provided");
    if (provided != null) {
      provided.getFiles().forEach(cp::add);
      ;
    }

    return cp;
  }

  public Set<File> sources() {
    SourceSet sourceSet = sourceSet(project);

    Set<File> src = new LinkedHashSet<>();
    // conf & public
    sourceSet.getResources().getSrcDirs().forEach(src::add);

    // source java
    sourceSet.getJava().getSrcDirs().forEach(src::add);

    return src;
  }

  public File javaSrc() {
    SourceSet sourceSet = sourceSet(project);

    // source java
    return sourceSet.getJava().getSrcDirs().iterator().next();
  }

  private SourceSet sourceSet(final Project project) {
    SourceSet sourceSet = getJavaConvention(project).getSourceSets()
        .getByName(SourceSet.MAIN_SOURCE_SET_NAME);
    return sourceSet;
  }

  public JavaPluginConvention getJavaConvention(final Project project) {
    return project.getConvention().getPlugin(JavaPluginConvention.class);
  }

  public URLClassLoader newClassLoader() throws MalformedURLException {
    return toClassLoader(
        classpath().stream()
            .map(f -> Try.of(() -> f.toURI().toURL()).get())
            .collect(Collectors.toList()),
        getClass().getClassLoader());
  }

  private static URLClassLoader toClassLoader(final List<URL> cp, final ClassLoader parent) {
    return new URLClassLoader(cp.toArray(new URL[cp.size()]), parent) {
      @Override
      public String toString() {
        return cp.toString();
      }
    };
  }
}
