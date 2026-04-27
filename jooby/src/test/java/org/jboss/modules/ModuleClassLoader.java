/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package org.jboss.modules;

public class ModuleClassLoader extends ClassLoader {
  public ModuleClassLoader() {
    super(ModuleClassLoader.class.getClassLoader());
  }
}
