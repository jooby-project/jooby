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
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
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

import com.google.common.base.Throwables;
import com.google.common.io.Files;

@Mojo(name = "assets", requiresDependencyResolution = ResolutionScope.TEST,
    defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
@Execute(phase = LifecyclePhase.PREPARE_PACKAGE)
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

  @SuppressWarnings("unchecked")
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    try {
      List<URL> cp = new ArrayList<>();
      cp.addAll(resources(mavenProject.getResources()));
      cp.add(output.toURI().toURL());
      cp.addAll(jars(mavenProject.getArtifacts()));

      // Hack and bind jooby env.
      System.setProperty("application.env", env);

      ClassLoader ctxloader = Thread.currentThread().getContextClassLoader();
      try (URLClassLoader loader = new URLClassLoader(cp.toArray(new URL[cp.size()]),
          getClass().getClassLoader())) {
        Thread.currentThread().setContextClassLoader(loader);
        Class<?> appClass = loader.loadClass(this.mainClass);
        Method on = appClass.getSuperclass().getDeclaredMethod("on",
            new Class[]{String.class, Consumer.class });
        Object app = appClass.newInstance();
        on.invoke(app, "*", compile(loader));
        Method start = appClass.getSuperclass().getDeclaredMethod("start", new Class[0]);
        start.invoke(app);
      } finally {
        Thread.currentThread().setContextClassLoader(ctxloader);
      }
    } catch (InvocationTargetException ex) {
      Throwable cause = ex.getCause();
      if (!(cause instanceof CompilationDone)) {
        throw new MojoFailureException("Can't compile assets for " + mainClass, cause);
      }
    } catch (Exception ex) {
      throw new MojoFailureException("Can't compile assets for " + mainClass, ex);
    }
  }

  private Consumer<Object> compile(final ClassLoader loader) {
    return conf -> {
      try {
        output.mkdirs();
        Class<?> ac = loader.loadClass("org.jooby.assets.AssetCompiler");
        Class<?> cclass = loader.loadClass("com.typesafe.config.Config");
        Object compiler = ac.getDeclaredConstructor(ClassLoader.class, cclass)
            .newInstance(loader, conf(loader, conf, cclass));
        @SuppressWarnings("unchecked")
        Map<String, List<File>> fileset = (Map<String, List<File>>) ac
            .getDeclaredMethod("build", String.class, File.class).invoke(compiler, env, output);
        StringBuilder min = new StringBuilder();
        min.append("assets.fileset {\n").append(fileset.entrySet().stream().map(e -> {
          String files = e.getValue().stream()
              .map(file -> output.toPath().relativize(file.toPath()))
              .map(path -> "/" + path.toString().replace("\\", "/"))
              .collect(Collectors.joining("\", \"", "[\"", "\"]"));
          return "  " + e.getKey() + ": " + files;
        }).collect(Collectors.joining("\n")))
            .append("\n}\n");
        min.append("assets.cache = ").append(maxAge).append("\n");
        min.append("assets.pipeline.dev = {}\n");
        min.append("assets.pipeline.").append(env).append(" = {}\n");
        File minFile = new File(output, "assets." + env + ".conf");
        FileWriter writer = new FileWriter(minFile);
        writer.write(min.toString());
        writer.close();
        getLog().info("done: " + minFile.getPath());

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

  private Object conf(final ClassLoader loader, final Object conf, final Class<?> cconfclass)
      throws Exception {
    /**
     *
     * ConfigFactory.parseResources(loader, "assets.conf")
     * .withFallback(conf)
     * .resolve();
     */
    Class<?> cfactory = loader.loadClass("com.typesafe.config.ConfigFactory");

    Method parseResources = cfactory.getDeclaredMethod("parseResources", ClassLoader.class,
        String.class);
    Object result = parseResources.invoke(null, loader, "assets.conf");
    Method withFallback = cconfclass.getDeclaredMethod("withFallback",
        cconfclass.getInterfaces()[0]);
    result = withFallback.invoke(result, conf);
    Method resolve = cconfclass.getDeclaredMethod("resolve");
    return resolve.invoke(result);
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
