/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.run;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.jboss.modules.ModuleLoader;
import org.jboss.modules.ModuleSpec;

/**
 * Creates a single/flat class loader where classes, resources and dependencies are all visible.
 * This is how joobyRun works until 3.x. It works without issue just is a lot slower on restart and
 * consumes more memory.
 */
public class JoobySingleModuleLoader extends JoobyModuleFinder {
  public JoobySingleModuleLoader(
      String name, Set<Path> classes, Set<Path> resources, Set<Path> jars, Set<Path> watchDirs) {
    super(name, classes, resources, jars, watchDirs);
  }

  @Override
  public ModuleSpec findModule(String name, ModuleLoader delegateLoader) {
    if (this.main.equals(name)) {
      Set<Path> classpath = new LinkedHashSet<>();
      classpath.addAll(classes);
      classpath.addAll(resources);
      classpath.addAll(jars);
      return createModuleSpec(name, classpath, Collections.emptySet());
    }
    return null;
  }
}
