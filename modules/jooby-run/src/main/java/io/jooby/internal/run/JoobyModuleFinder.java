/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.run;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.joining;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;

import org.jboss.modules.ModuleFinder;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.ModuleSpec;

import io.jooby.run.JoobyRun;

public class JoobyModuleFinder implements ModuleFinder {
  private static final String JARS = "jars";
  private static final String RESOURCES = "resources";
  private final Set<Path> classes;
  private final Set<Path> resources;
  private final Set<Path> jars;
  private final String main;
  private final Set<Path> watchDirs;

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

  /**
   * Allow access to application classloader to joobyRun hook class: io.jooby.run.ServerRef.
   *
   * @param loader Loader.
   * @return Path on file system of jooby-run-XXX.jar.
   */
  static Path joobyRunHook(Class loader) {
    try {
      URL serverRef = loader.getResource("/" + JoobyRun.SERVER_REF.replace(".", "/") + ".class");
      JarURLConnection connection = (JarURLConnection) serverRef.openConnection();
      return Paths.get(connection.getJarFileURL().toURI());
    } catch (IOException | URISyntaxException x) {
      throw new IllegalStateException("jooby-run.jar not found", x);
    }
  }

  @Override
  public ModuleSpec findModule(String name, ModuleLoader delegateLoader) {
    var resources = resources(name);
    if (resources == null) {
      return null;
    }
    return ModuleSpecHelper.create(name, resources, dependencies(name, true));
  }

  public Set<String> dependencies(String name) {
    return dependencies(name, false);
  }

  private Set<String> dependencies(String name, boolean addResources) {
    if (this.main.equals(name)) {
      // class only; depends on resources + jars
      return addResources ? Set.of(RESOURCES, JARS) : Set.of(JARS);
    } else if (JARS.equals(name)) {
      // jars depends on main when reflection is required (hibernate, jackson, quartz);
      return addResources ? Set.of(RESOURCES, main) : Set.of(main);
    }
    return emptySet();
  }

  private Set<Path> resources(String name) {
    if (this.main.equals(name)) {
      return classes;
    } else if (RESOURCES.equals(name)) {
      return resources;
    } else if (JARS.equals(name)) {
      return jars;
    }
    return null;
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
