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
import java.io.FileWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.gradle.api.internal.ConventionTask;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.jooby.assets.AssetCompiler;

import com.google.common.base.Throwables;
import com.google.common.io.Files;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class AssetTask extends ConventionTask {

  @SuppressWarnings("serial")
  private static class CompilationDone extends RuntimeException {
  }

  private String env;

  private String mainClassName;

  @InputDirectory
  private File output;

  @OutputDirectory
  private File assemblyOutput;

  private String maxAge;

  @OutputFile
  private File assetFile;

  @TaskAction
  public void process() throws Throwable {
    long start = System.currentTimeMillis();
    try {
      // Hack and bind jooby env.
      String env = getEnv();
      this.assetFile = new File(getOutput(), "assets." + env + ".conf");
      new JoobyRunner(getProject())
          .run(getMainClassName(), app -> {
            app.on("*",
                compile(getLogger(), app.getClass().getClassLoader(), env, getMaxAge(), getOutput(),
                    assetFile, getAssemblyOutput()));
          }, env);
    } catch (CompilationDone ex) {
      long end = System.currentTimeMillis();
      getLogger().info("compilation took " + (end - start) + "ms");
    }
  }

  private static Consumer<Config> compile(final Logger logger, final ClassLoader loader,
      final String env, final String maxAge, final File output, final File distFile,
      final File assemblyOutput) {
    return conf -> {
      try {
        output.mkdirs();

        logger.debug("claspath: " + loader);

        Config assetConf = ConfigFactory.parseResources(loader, "assets.conf")
            .withFallback(conf);

        logger.debug("assets.conf: " + assetConf.getConfig("assets"));

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
        try (FileWriter writer = new FileWriter(distFile)) {
          writer.write(dist.toString());
        }
        logger.info("done: " + distFile.getPath());

        // move output to fixed location required by zip/war dist
        List<File> files = fileset.values().stream()
            .flatMap(it -> it.stream())
            .collect(Collectors.toList());

        for (File from : files) {
          File to = assemblyOutput.toPath().resolve(output.toPath().relativize(from.toPath()))
              .toFile();
          to.getParentFile().mkdirs();
          logger.debug("copying file to: " + to);
          Files.copy(from, to);
        }
      } catch (InvocationTargetException ex) {
        Throwables.propagate(ex.getCause());
      } catch (Exception ex) {
        Throwables.propagate(ex);
      }
      // signal we are done
      throw new CompilationDone();
    };
  }

  public String getEnv() {
    return env;
  }

  public void setEnv(final String env) {
    this.env = env;
  }

  public String getMainClassName() {
    return mainClassName;
  }

  public void setMainClassName(final String mainClassName) {
    this.mainClassName = mainClassName;
  }

  public File getOutput() {
    return output;
  }

  public void setOutput(final File output) {
    this.output = output;
  }

  public File getAssemblyOutput() {
    return assemblyOutput;
  }

  public void setAssemblyOutput(final File assemblyOutput) {
    this.assemblyOutput = assemblyOutput;
  }

  public String getMaxAge() {
    return maxAge;
  }

  public void setMaxAge(final String maxAge) {
    this.maxAge = maxAge;
  }

}
