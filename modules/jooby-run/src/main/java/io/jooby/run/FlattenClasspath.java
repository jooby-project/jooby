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

import java.nio.file.Path;
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
    this.resources.addAll(dependencies);
  }

  @Override public ModuleSpec findModule(String name, ModuleLoader delegateLoader)
      throws ModuleLoadException {
    if (this.name.equals(name)) {
      return Specs.spec(name, resources, Collections.emptySet());
    }

    return null;
  }
}
