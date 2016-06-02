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
import java.io.FileWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
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
import org.jooby.assets.AssetCompiler;

import com.google.common.base.Throwables;
import com.google.common.io.Files;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

@Mojo(name = "assets", defaultPhase = LifecyclePhase.COMPILE,
    requiresDependencyResolution = ResolutionScope.TEST)
@Execute(phase = LifecyclePhase.COMPILE)
public class AssetMojo extends AbstractMojo {

  @SuppressWarnings("serial")
  private static class CompilationDone extends RuntimeException {
  }

  @Component
  private MavenProject mavenProject;

  @Parameter(property = "main.class", defaultValue = "${application.class}")
  protected String mainClass;

  @Parameter(defaultValue = "${project.build.outputDirectory}")
  private File output;

  @Parameter(defaultValue = "${project.build.directory}${file.separator}__public_")
  private File assemblyOutput;

  @Parameter(defaultValue = "dist")
  private String env;

  @Parameter(defaultValue = "365d")
  private String maxAge;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    long start = System.currentTimeMillis();
    try {
      // Hack and bind jooby env.
      System.setProperty("application.env", env);

      new JoobyRunner(mavenProject)
          .run(mainClass, app -> {
            app.on("*", compile(app.getClass().getClassLoader()));
          });
    } catch (CompilationDone ex) {
      long end = System.currentTimeMillis();
      getLog().info("compilation took " + (end - start) + "ms");
    } catch (Throwable ex) {
      throw new MojoFailureException("Can't compile assets for " + mainClass, ex);
    }
  }

  private Consumer<Config> compile(final ClassLoader loader) {
    return conf -> {
      try {
        output.mkdirs();

        getLog().debug("claspath: " + loader);

        Config assetConf = ConfigFactory.parseResources(loader, "assets.conf")
            .withFallback(conf);

        getLog().debug("assets.conf: " + assetConf.getConfig("assets"));

        AssetCompiler compiler = new AssetCompiler(loader, assetConf);

        Map<String, List<File>> fileset = compiler.build(env, output);

        StringBuilder dist = new StringBuilder();
        dist.append("assets.fileset {\n").append(fileset.entrySet().stream().map(e -> {
          String files = e.getValue().stream()
              .map(file -> output.toPath().relativize(file.toPath()))
              .map(path -> "/" + path.toString().replace("\\", "/"))
              .collect(Collectors.joining("\", \"", "[\"", "\"]"));
          return "  " + e.getKey() + ": " + files;
        }).collect(Collectors.joining("\n")))
            .append("\n}\n");
        dist.append("assets.cache.maxAge = ").append(maxAge).append("\n");
        dist.append("assets.pipeline.dev = {}\n");
        dist.append("assets.pipeline.").append(env).append(" = {}\n");
        dist.append("assets.watch = false\n");
        File distFile = new File(output, "assets." + env + ".conf");
        try (FileWriter writer = new FileWriter(distFile)) {
          writer.write(dist.toString());
        }
        getLog().info("done: " + distFile.getPath());

        // move output to fixed location required by zip/war dist
        List<File> files = fileset.values().stream()
            .flatMap(it -> it.stream())
            .collect(Collectors.toList());

        for (File from : files) {
          File to = assemblyOutput.toPath().resolve(output.toPath().relativize(from.toPath()))
              .toFile();
          to.getParentFile().mkdirs();
          getLog().debug("copying file to: " + to);
          Files.copy(from, to);
        }
      } catch (InvocationTargetException ex) {
        throw Throwables.propagate(ex.getCause());
      } catch (Exception ex) {
        throw Throwables.propagate(ex);
      }
      // signal we are done
      throw new CompilationDone();
    };
  }

}
