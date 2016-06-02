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
package org.jooby;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;

public class Classpath {

  private MavenProject project;

  public Classpath(final MavenProject project) {
    this.project = project;
  }

  @SuppressWarnings("unchecked")
  public List<URL> build() throws MalformedURLException {
    List<URL> cp = new ArrayList<>();
    cp.addAll(resources(project.getResources()));
    cp.add(new File(project.getBuild().getOutputDirectory()).toURI().toURL());
    cp.addAll(jars(project.getArtifacts()));
    return cp;
  }

  public URLClassLoader toClassLoader() throws MalformedURLException {
    return toClassLoader(build(), getClass().getClassLoader());
  }

  private static URLClassLoader toClassLoader(final List<URL> cp, final ClassLoader parent) {
    return new URLClassLoader(cp.toArray(new URL[cp.size()]), parent) {
      @Override
      public String toString() {
        return cp.toString();
      }
    };
  }

  private List<URL> jars(final Iterable<Artifact> artifacts) throws MalformedURLException {
    List<URL> result = new ArrayList<URL>();
    for (Artifact artifact : artifacts) {
      if (!"pom".equals(artifact.getType())) {
        result.add(artifact.getFile().toURI().toURL());
      }
    }
    return result;
  }

  private List<URL> resources(final Iterable<Resource> resources) throws MalformedURLException {
    List<URL> result = new ArrayList<URL>();
    for (Resource resource : resources) {
      File dir = new File(resource.getDirectory());
      if (dir.exists()) {
        result.add(dir.toURI().toURL());
      }
    }
    return result;
  }

}
