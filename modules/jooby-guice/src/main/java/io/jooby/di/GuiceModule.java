/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.di;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import io.jooby.Environment;
import io.jooby.Extension;
import io.jooby.Jooby;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class GuiceModule implements Extension {

  private Injector injector;

  private List<Module> modules = new ArrayList<>();

  public GuiceModule(@Nonnull Injector injector) {
    this.injector = injector;
  }

  public GuiceModule(@Nonnull Module... modules) {
    Stream.of(modules).forEach(this.modules::add);
  }

  @Override public boolean lateinit() {
    return true;
  }

  @Override public void install(@Nonnull Jooby application) {
    if (injector == null) {
      Environment env = application.getEnvironment();
      modules.add(new JoobyModule(application));
      Stage stage = env.isActive("dev", "test") ? Stage.DEVELOPMENT : Stage.PRODUCTION;
      injector = Guice.createInjector(stage, modules);
    }
    application.registry(new GuiceRegistry(injector));
  }
}
