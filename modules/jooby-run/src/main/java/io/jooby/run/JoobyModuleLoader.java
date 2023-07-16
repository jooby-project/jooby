/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.run;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleFinder;
import org.jboss.modules.ModuleLoader;

class JoobyModuleLoader extends ModuleLoader {

  JoobyModuleLoader(ModuleFinder finder) {
    super(finder);
  }

  public void unload(String name, final Module module) {
    super.unloadModuleLocal(name, module);
  }
}
