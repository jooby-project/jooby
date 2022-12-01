/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.camel;

import org.apache.camel.spi.PropertiesSource;

import com.typesafe.config.Config;

public class JoobyPropertiesSource implements PropertiesSource {
  private Config config;

  public JoobyPropertiesSource(Config config) {
    this.config = config;
  }

  @Override
  public String getName() {
    return config.origin().toString();
  }

  @Override
  public String getProperty(String name) {
    return config.hasPath(name) ? config.getString(name) : null;
  }
}
