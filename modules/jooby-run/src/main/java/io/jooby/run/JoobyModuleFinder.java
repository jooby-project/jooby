/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.run;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

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

class JoobyModuleFinder implements ModuleFinder {
  private static final String JARS = "jars";
  private final Set<Path> resources;
  private final Set<Path> jars;
  private final String name;

  JoobyModuleFinder(String name, Set<Path> resources, Set<Path> jars) {
    this.name = name;
    this.resources = new LinkedHashSet<>(resources.size() + 1);
    this.resources.addAll(resources);
    this.resources.add(joobyRunHook(getClass()));

    this.jars = jars;
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
      return ModuleSpecHelper.create(name, resources, singleton(JARS));
    } else if (JARS.equals(name)) {
      return ModuleSpecHelper.create(name, jars, emptySet());
    }

    return null;
  }

  @Override
  public String toString() {
    var buffer = new StringBuilder();
    resources.stream()
        .forEach(
            it -> {
              if (!buffer.isEmpty()) {
                buffer.append(File.pathSeparator);
              }
              buffer.append(it);
            });
    if (!jars.isEmpty()) {
      buffer.append("; jars: ").append(jars);
    }
    return buffer.toString();
  }
}
