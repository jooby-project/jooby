/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.run;

import org.jboss.modules.ModuleFinder;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.ModuleSpec;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

class FlattenClasspath implements ModuleFinder {

  private final Set<Path> resources;
  private final String name;

  FlattenClasspath(String name, Set<Path> resources, Set<Path> dependencies) {
    this.name = name;
    this.resources = new LinkedHashSet<>(resources.size() + dependencies.size());
    this.resources.addAll(resources);

    this.resources.add(joobyRunHook(getClass()));
    this.resources.addAll(dependencies);
  }

  /**
   * Allow access to application classloader to joobyRun hook class: io.jooby.run.ServerRef.
   *
   * @param loader Loader.
   * @return Path on file system of jooby-run-XXX.jar.
   */
  static Path joobyRunHook(Class loader) {
    try {
      URL serverRef = loader
          .getResource("/" + JoobyRun.SERVER_REF.replace(".", "/") + ".class");
      JarURLConnection connection = (JarURLConnection) serverRef.openConnection();
      return Paths.get(connection.getJarFileURL().toURI());
    } catch (IOException | URISyntaxException x) {
      throw new IllegalStateException("jooby-run.jar not found", x);
    }
  }

  @Override public ModuleSpec findModule(String name, ModuleLoader delegateLoader)
      throws ModuleLoadException {
    if (this.name.equals(name)) {
      return Specs.spec(name, resources, Collections.emptySet());
    }

    return null;
  }
}
