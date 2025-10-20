/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.run;

import static java.util.Collections.emptySet;

import java.nio.file.Path;
import java.util.Set;

import org.jboss.modules.ModuleLoader;
import org.jboss.modules.ModuleSpec;

/**
 * The new class loader since 3.x. It creates 3 modules with their own classloader:
 *
 * <ul>
 *   <li>classes: project classes
 *   <li>resources: project resources
 *   <li>jars: project dependencies
 * </ul>
 *
 * <p>This approach reduce memory footprint allowing fast restart.
 */
public class JoobyMultiModuleFinder extends JoobyModuleFinder {

  public JoobyMultiModuleFinder(
      String name, Set<Path> classes, Set<Path> resources, Set<Path> jars, Set<Path> watchDirs) {
    super(name, classes, resources, jars, watchDirs);
  }

  @Override
  public ModuleSpec findModule(String name, ModuleLoader delegateLoader) {
    var resources = resources(name);
    if (resources == null) {
      return null;
    }
    return createModuleSpec(name, resources, dependencies(name, true));
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
}
