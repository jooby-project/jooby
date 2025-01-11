/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.avaje.inject;

import java.util.List;
import java.util.stream.Collectors;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.avaje.inject.BeanScope;
import io.avaje.inject.BeanScopeBuilder;
import io.jooby.Environment;
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
 *   install(AvajeInjectModule.of());
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

  public AvajeInjectModule(@NonNull BeanScopeBuilder beanScope) {
    this.beanScope = beanScope;
  }

  @Override
  public boolean lateinit() {
    return true;
  }

  @Override
  public void install(Jooby application) {

    application
        .getServices()
        .entrySet()
        .forEach(
            e -> {
              var key = e.getKey();
              var provider = e.getValue();
              beanScope.bean(key.getName(), key.getType(), provider);
            });

    final var environment = application.getEnvironment();

    beanScope.bean(Environment.class, environment);
    beanScope.profiles(environment.getActiveNames().toArray(String[]::new));

    // configuration properties
    final var config = environment.getConfig();
    beanScope.configPlugin(new JoobyPropertyPlugin(config));

    for (var entry : config.entrySet()) {
      String name = entry.getKey();
      Object value = entry.getValue().unwrapped();

      if (value instanceof List<?> values) {
        value = values.stream().map(Object::toString).collect(Collectors.joining(","));
      }
      beanScope.bean(name, String.class, value.toString());
    }

    application.registry(new AvajeInjectRegistry(beanScope.build()));
  }
}
