/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.guice;

import com.google.inject.ConfigurationException;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.ProvisionException;
import com.google.inject.name.Names;
import io.jooby.Registry;
import io.jooby.Reified;
import io.jooby.ServiceKey;
import io.jooby.exception.RegistryException;

class GuiceRegistry implements Registry {
  private final Injector injector;

  GuiceRegistry(Injector injector) {
    this.injector = injector;
  }

  @Override
  public <T> T require(Class<T> type) {
    return require(Key.get(type));
  }

  @Override
  public <T> T require(Class<T> type, String name) {
    return require(Key.get(type, Names.named(name)));
  }

  @Override
  public <T> T require(Reified<T> type) throws RegistryException {
    //noinspection unchecked
    return (T) require(Key.get(type.getType()));
  }

  @Override
  public <T> T require(Reified<T> type, String name) throws RegistryException {
    //noinspection unchecked
    return (T) require(Key.get(type.getType(), Names.named(name)));
  }

  @Override
  public <T> T require(ServiceKey<T> key) throws RegistryException {
    String name = key.getName();
    //noinspection unchecked
    return name == null
        ? (T) require(Key.get(key.getType()))
        : (T) require(Key.get(key.getType(), Names.named(name)));
  }

  private <T> T require(Key<T> key) {
    try {
      return injector.getInstance(key);
    } catch (ProvisionException | ConfigurationException x) {
      throw new RegistryException("Provisioning of `" + key + "` resulted in exception", x);
    }
  }
}
