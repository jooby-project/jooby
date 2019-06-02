/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.di;

import io.jooby.Extension;
import io.jooby.Jooby;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;

import javax.annotation.Nonnull;

import static org.jboss.weld.environment.se.Weld.SHUTDOWN_HOOK_SYSTEM_PROPERTY;

public class Weldby implements Extension {

  private WeldContainer container;

  private String[] packages;

  public Weldby(@Nonnull WeldContainer container) {
    this.container = container;
  }

  public Weldby(@Nonnull String... packages) {
    this.packages = packages;
    this.container = null;
  }

  @Override public boolean lateinit() {
    return true;
  }

  @Override public void install(@Nonnull Jooby application) {
    if (container == null) {
      if (packages == null || packages.length == 0) {
        String basePackage = application.getBasePackage();
        if (basePackage == null) {
          throw new IllegalStateException("Weld requires at least one package to scan.");
        }
        packages = new String[]{basePackage};
      }
      Weld weld = new Weld()
          .disableDiscovery()
          .addPackages(true, toPackages(packages))
          .addProperty(SHUTDOWN_HOOK_SYSTEM_PROPERTY, false)
          .addExtension(new JoobyExtension(application));

      application.onStop(weld::shutdown);

      container = weld.initialize();
    }

    application.registry(new WeldRegistry(container));
  }

  private static Package[] toPackages(String[] packages) {
    Package[] result = new Package[packages.length];
    for (int i = 0; i < packages.length; i++) {
      result[i] = Package.getPackage(packages[i]);
    }
    return result;
  }
}
