/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.di;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import org.springframework.core.env.PropertySource;

public class ConfigPropertySource extends PropertySource<Config> {
  public ConfigPropertySource(String name, Config source) {
    super(name, source);
  }

  @Override public boolean containsProperty(String key) {
    try {
      return source.hasPath(key);
    } catch (ConfigException x) {
      return false;
    }
  }

  @Override public Object getProperty(String key) {
    return containsProperty(key) ? source.getAnyRef(key) : null;
  }
}
