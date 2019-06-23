/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.di;

import com.google.inject.ConfigurationException;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.ProvisionException;
import com.google.inject.name.Names;
import io.jooby.Registry;
import io.jooby.RegistryException;
import io.jooby.ServiceKey;

import javax.annotation.Nonnull;

public class GuiceRegistry implements Registry {
  private Injector injector;

  public GuiceRegistry(Injector injector) {
    this.injector = injector;
  }

  @Nonnull @Override public <T> T require(@Nonnull Class<T> type) {
    return require(Key.get(type));
  }

  @Nonnull @Override public <T> T require(@Nonnull Class<T> type, @Nonnull String name) {
    return require(Key.get(type, Names.named(name)));
  }

  @Nonnull @Override public <T> T require(@Nonnull ServiceKey<T> key) throws RegistryException {
    String name = key.getName();
    return name == null ? require(key.getType()) : require(key.getType(), name);
  }

  @Nonnull private <T> T require(@Nonnull Key<T> key) {
    try {
      return injector.getInstance(key);
    } catch (ProvisionException | ConfigurationException x) {
      throw new RegistryException("Provisioning of `" + key + "` resulted in exception", x);
    }
  }
}
