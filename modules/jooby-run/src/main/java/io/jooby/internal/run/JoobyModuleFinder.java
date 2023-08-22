/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.run;

import static java.util.stream.Collectors.joining;
import static org.jboss.modules.ResourceLoaderSpec.createResourceLoaderSpec;
import static org.jboss.modules.ResourceLoaders.createJarResourceLoader;
import static org.jboss.modules.ResourceLoaders.createPathResourceLoader;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarFile;

import org.jboss.modules.DependencySpec;
import org.jboss.modules.ModuleDependencySpecBuilder;
import org.jboss.modules.ModuleFinder;
import org.jboss.modules.ModuleSpec;
import org.jboss.modules.PathUtils;
import org.jboss.modules.ResourceLoaderSpec;
import org.jboss.modules.filter.PathFilters;

import io.jooby.run.JoobyRun;

public abstract class JoobyModuleFinder implements ModuleFinder {
  protected static final String JARS = "jars";
  protected static final String RESOURCES = "resources";
  protected final Set<Path> classes;
  protected final Set<Path> resources;
  protected final Set<Path> jars;
  protected final String main;
  protected final Set<Path> watchDirs;

  public JoobyModuleFinder(
      String name, Set<Path> classes, Set<Path> resources, Set<Path> jars, Set<Path> watchDirs) {
    this.main = name;
    this.classes = classes;
    this.resources = resources;
    this.jars = new LinkedHashSet<>(resources.size() + 1);
    this.jars.add(joobyRunHook(getClass()));
    this.jars.addAll(jars);
    this.watchDirs = watchDirs;
  }

  public Set<String> dependencies(String name) {
    return Collections.emptySet();
  }

  /**
   * Allow access to application classloader to joobyRun hook class: io.jooby.run.ServerRef.
   *
   * @param loader Loader.
   * @return Path on file system of jooby-run-XXX.jar.
   */
  private static Path joobyRunHook(Class loader) {
    try {
      URL serverRef = loader.getResource("/" + JoobyRun.SERVER_REF.replace(".", "/") + ".class");
      JarURLConnection connection = (JarURLConnection) serverRef.openConnection();
      return Paths.get(connection.getJarFileURL().toURI());
    } catch (IOException | URISyntaxException x) {
      throw new IllegalStateException("jooby-run.jar not found", x);
    }
  }

  public static ModuleSpec createModuleSpec(
      String name, Set<Path> resources, Set<String> dependencies) {
    ModuleSpec.Builder builder = newModule(name, resources);

    // dependencies
    for (String dependency : dependencies) {
      builder.addDependency(
          new ModuleDependencySpecBuilder()
              .setImportFilter(PathFilters.acceptAll())
              .setExportFilter(PathFilters.getMetaInfServicesFilter())
              .setName(dependency)
              .setOptional(false)
              .build());
    }
    return builder.create();
  }

  private static ModuleSpec.Builder newModule(String name, Set<Path> resources) {
    try {
      ModuleSpec.Builder builder = ModuleSpec.build(name);
      // Add all JDK classes
      builder.addDependency(DependencySpec.createSystemDependencySpec(PathUtils.getPathSet(null)));
      // needed, so that the module can load classes from the resource root
      builder.addDependency(DependencySpec.createLocalDependencySpec());
      // Add the module's own content
      builder.addDependency(DependencySpec.OWN_DEPENDENCY);

      for (Path path : resources) {
        if (Files.isDirectory(path)) {
          builder.addResourceRoot(
              ResourceLoaderSpec.createResourceLoaderSpec(createPathResourceLoader(path)));
        } else {
          builder.addResourceRoot(
              createResourceLoaderSpec(createJarResourceLoader(new JarFile(path.toFile()))));
        }
      }
      return builder;
    } catch (IOException x) {
      throw JoobyRun.sneakyThrow0(x);
    }
  }

  @Override
  public String toString() {
    return "classes: "
        + classes.stream().map(Path::toString).collect(joining(File.pathSeparator))
        + "\nresources: "
        + resources.stream().map(Path::toString).collect(joining(File.pathSeparator))
        + "\nwatchDirs: "
        + watchDirs.stream().map(Path::toString).collect(joining(File.pathSeparator))
        + "\njars: "
        + jars.stream().map(Path::getFileName).map(Path::toString).collect(joining(", "));
  }
}
