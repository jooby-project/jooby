/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.avaje.inject;

import com.typesafe.config.Config;

import io.avaje.inject.BeanScope;
import io.avaje.inject.BeanScopeBuilder;
import io.jooby.Extension;
import io.jooby.Jooby;

/**
 * Avaje Inject module: https://jooby.io/modules/avaje-inject.
 *
 * <p>Jooby integrates the {@link io.jooby.ServiceRegistry} into the Avaje DI framework.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * {
 *
 *
 *   install(new AvajeInjectModule());
 *
 * }
 *
 * }</pre>
 *
 * Require calls are going to be resolved by Avaje inject now.
 *
 * @author josiah
 * @since 3.0.0
 */
public class AvajeInjectModule implements Extension {

  private final BeanScopeBuilder beanScope;

  public static AvajeInjectModule of() {
    return new AvajeInjectModule(BeanScope.builder());
  }

  public static AvajeInjectModule of(BeanScopeBuilder beanScope) {
    return new AvajeInjectModule(beanScope);
  }

  AvajeInjectModule(BeanScopeBuilder beanScope) {
    this.beanScope = beanScope;
  }

  @Override
  public void install(Jooby application) throws Exception {

    application
        .getServices()
        .entrySet()
        .forEach(
            e -> {
              final var key = e.getKey();
              beanScope.provideDefault(key.getName(), key.getType(), e::getValue);
            });
    final var environment = application.getEnvironment();
    beanScope.profiles(environment.getActiveNames().toArray(String[]::new));
    beanScope.propertyPlugin(new JoobyPropertyPlugin(environment.getConfig()));
    beanScope.bean(Config.class, environment.getConfig());

    application.registry(new AvajeInjectRegistry(beanScope.build()));
  }
}
