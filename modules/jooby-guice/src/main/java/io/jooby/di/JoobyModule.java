/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.di;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.google.inject.util.Types;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;
import io.jooby.Environment;
import io.jooby.Jooby;
import io.jooby.ServiceKey;
import io.jooby.ServiceRegistry;

import javax.annotation.Nonnull;
import javax.inject.Provider;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Exposes Jooby objects to Guice. This module exposes {@link Environment}, {@link Config} and
 * application services.
 *
 * @author edgar
 * @since 2.0.0
 */
public class JoobyModule extends AbstractModule {
  private Jooby application;

  /**
   * Creates a new Jooby Module.
   *
   * @param application Jooby application.
   */
  public JoobyModule(@Nonnull Jooby application) {
    this.application = application;
  }

  @Override protected void configure() {
    configureEnv(application.getEnvironment());
    configureResources(application.getServices());
  }

  private void configureResources(ServiceRegistry registry) {
    // bind the available resources as well, supporting the name annotations that may be set
    for (Map.Entry<ServiceKey<?>, Provider<?>> entry : registry.entrySet()) {
      ServiceKey<?> key = entry.getKey();
      Provider provider = entry.getValue();
      LinkedBindingBuilder<?> binding;
      if (key.getName() != null) {
        binding = bind(key.getType()).annotatedWith(Names.named(key.getName()));
      } else {
        binding = bind(key.getType());
      }
      binding.toProvider(provider);
    }
  }

  private void configureEnv(Environment env) {
    Config config = env.getConfig();
    bind(Config.class).toInstance(config);
    bind(Environment.class).toInstance(env);

    // configuration properties
    for (Map.Entry<String, ConfigValue> entry : config.entrySet()) {
      String name = entry.getKey();
      Named named = Names.named(name);
      Object value = entry.getValue().unwrapped();
      if (value instanceof List) {
        List values = (List) value;
        Type listType = Types.listOf(values.iterator().next().getClass());
        Key key = Key.get(listType, Names.named(name));
        bind(key).toInstance(values);
        value = values.stream().map(Object::toString).collect(Collectors.joining(","));
      }
      bindConstant().annotatedWith(named).to(value.toString());
    }
  }
}
