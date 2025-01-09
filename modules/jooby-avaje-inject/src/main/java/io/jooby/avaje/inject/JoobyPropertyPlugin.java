/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.avaje.inject;

import java.util.Objects;
import java.util.Optional;

import com.typesafe.config.Config;
import io.avaje.inject.spi.ConfigPropertyPlugin;

class JoobyPropertyPlugin implements ConfigPropertyPlugin {

  private final Config config;

  public JoobyPropertyPlugin(Config config) {
    this.config = config;
  }

  @Override
  public Optional<String> get(String property) {
    return config.hasPath(property)
        ? Optional.of(config.getString(property))
        : Optional.empty();
  }

  @Override
  public boolean contains(String property) {
    return config.hasPath(property);
  }

  @Override
  public boolean equalTo(String property, String value) {
    return config.hasPath(property) && Objects.equals(config.getString(property), value);
  }
}
