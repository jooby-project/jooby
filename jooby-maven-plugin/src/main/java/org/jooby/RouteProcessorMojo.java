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
import java.nio.file.Path;
import java.util.stream.Collectors;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.jooby.spec.RouteProcessor;
import org.jooby.spec.RouteSpec;

@Mojo(name = "spec", requiresDependencyResolution = ResolutionScope.COMPILE,
    defaultPhase = LifecyclePhase.PROCESS_CLASSES)
@Execute(phase = LifecyclePhase.PROCESS_CLASSES)
public class RouteProcessorMojo extends AbstractMojo {

  @SuppressWarnings("serial")
  private static class ProcessDone extends RuntimeException {
  }

  @Component
  private MavenProject mavenProject;

  @Parameter(property = "main.class", defaultValue = "${application.class}")
  protected String mainClass;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    try {
      Path srcdir = new File(mavenProject.getBuild().getSourceDirectory()).toPath();
      Path bindir = new File(mavenProject.getBuild().getOutputDirectory()).toPath();
      new JoobyRunner(mavenProject)
          .run(mainClass, app -> {
            process(app, srcdir, bindir);
          });
    } catch (ProcessDone ex) {
    } catch (Exception ex) {
      throw new MojoFailureException("Can't build route spec for: " + mainClass, ex);
    }
  }

  private void process(final Jooby app, final Path srcdir, final Path bindir) {
    RouteProcessor processor = new RouteProcessor();
    String[] name = app.getClass().getPackage().getName().split("\\.");
    Path outdir = bindir;
    for (String n : name) {
      outdir = outdir.resolve(n);
    }
    String routes = processor.compile(app, srcdir, outdir).stream()
        .map(RouteSpec::toString)
        .collect(Collectors.joining("\n"));
    getLog().debug(app.getClass().getSimpleName() + ".spec :\n" + routes);
    throw new ProcessDone();
  }

}
