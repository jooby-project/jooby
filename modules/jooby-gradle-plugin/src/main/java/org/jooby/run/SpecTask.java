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
import java.nio.file.Path;
import java.util.stream.Collectors;

import org.gradle.api.Project;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.jooby.Jooby;
import org.jooby.spec.RouteProcessor;
import org.jooby.spec.RouteSpec;

public class SpecTask extends ConventionTask {

  @SuppressWarnings("serial")
  private static class Done extends RuntimeException {
  }

  private String mainClassName;

  @InputDirectory
  private File source;

  @TaskAction
  public void process() throws Throwable {
    long start = System.currentTimeMillis();
    try {
      Project project = getProject();
      new JoobyContainer(project)
          .run(getMainClassName(), app -> {
            process(app, getSource().toPath(), new JoobyProject(project).buildResources().toPath());
          });
    } catch (Done ex) {
      long end = System.currentTimeMillis();
      getLogger().info("compilation took " + (end - start) + "ms");
    }
  }

  private void process(final Jooby app, final Path srcdir, final Path bindir) {
    getLogger().info("Source: {}", srcdir);
    getLogger().info("Output: {}", bindir);
    RouteProcessor processor = new RouteProcessor();
    String[] name = app.getClass().getPackage().getName().split("\\.");
    Path outdir = bindir;
    for (String n : name) {
      outdir = outdir.resolve(n);
    }
    String routes = processor.compile(app, srcdir, outdir).stream()
        .map(RouteSpec::toString)
        .collect(Collectors.joining("\n"));
    getLogger().debug(app.getClass().getSimpleName() + ".spec :\n" + routes);
    // update task output
    throw new Done();
  }

  public File getSource() {
    return source;
  }

  public void setSource(final File source) {
    this.source = source;
  }

  public String getMainClassName() {
    return mainClassName;
  }

  public void setMainClassName(final String mainClassName) {
    this.mainClassName = mainClassName;
  }

}
