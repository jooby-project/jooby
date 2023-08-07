/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.run;

import static java.util.Collections.emptySet;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

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
  private final String name;

  public JoobyModuleFinder(String name, Set<Path> classes, Set<Path> resources, Set<Path> jars) {
    this.name = name;
    this.classes = classes;
    this.resources = resources;
    this.jars = new LinkedHashSet<>(resources.size() + 1);
    this.jars.add(joobyRunHook(getClass()));
    this.jars.addAll(jars);
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
    if (this.name.equals(name)) {
      // class only; depends on resources + jars
      return ModuleSpecHelper.create(name, classes, Set.of(RESOURCES, JARS));
    } else if (RESOURCES.equals(name)) {
      // resources only;
      return ModuleSpecHelper.create(name, resources, emptySet());
    } else if (JARS.equals(name)) {
      // jars only; depends on resources
      return ModuleSpecHelper.create(name, jars, Set.of(RESOURCES));
    }

    return null;
  }

  @Override
  public String toString() {
    return "classes: "
        + classes.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator))
        + "\nresources: "
        + resources.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator))
        + "\njars: "
        + jars.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator));
  }
}
