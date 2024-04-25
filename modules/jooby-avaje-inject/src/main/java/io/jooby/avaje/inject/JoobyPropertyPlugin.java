package io.jooby.avaje.inject;

import java.util.Objects;
import java.util.Optional;

import com.typesafe.config.Config;

import io.avaje.inject.spi.PropertyRequiresPlugin;

public class JoobyPropertyPlugin implements PropertyRequiresPlugin {

  private final Config config;

  public JoobyPropertyPlugin(Config config) {
    this.config = config;
  }

  @Override
  public Optional<String> get(String property) {
    return Optional.ofNullable(config.getString(property));
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
