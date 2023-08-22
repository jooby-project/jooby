/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.run;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;

import io.jooby.SneakyThrows;

public class JoobyModuleLoader extends ModuleLoader {

  public JoobyModuleLoader(JoobyModuleFinder finder) {
    super(finder);
  }

  protected JoobyModuleFinder joobyModuleFinder() {
    return (JoobyMultiModuleFinder) getFinders()[0];
  }

  public void unload(String name, Module module) {
    super.unloadModuleLocal(name, module);
    var finder = joobyModuleFinder();
    // relink any dependency
    finder.dependencies(name).stream()
        .map(super::findLoadedModuleLocal)
        .forEach(SneakyThrows.throwingConsumer(super::relink));
  }

  public String toString() {
    return "joobyRun {\n" + getFinders()[0] + "\n}";
  }
}
