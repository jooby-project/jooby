/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.avaje.inject;

import java.util.NoSuchElementException;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.avaje.inject.BeanScope;
import io.jooby.Registry;
import io.jooby.ServiceKey;
import io.jooby.exception.RegistryException;

class AvajeInjectRegistry implements Registry {

  private final BeanScope beanScope;

  public AvajeInjectRegistry(BeanScope beanScope) {
    this.beanScope = beanScope;
  }

  @Override
  public @NonNull <T> T require(@NonNull Class<T> type) throws RegistryException {
    try {
      return beanScope.get(type);
    } catch (NoSuchElementException e) {
      ServiceKey<T> key = ServiceKey.key(type);
      throw new RegistryException("Provisioning of `" + key + "` resulted in exception", e);
    }
  }

  @Override
  public @NonNull <T> T require(@NonNull Class<T> type, @NonNull String name)
      throws RegistryException {
    try {
      return beanScope.get(type, name);
    } catch (NoSuchElementException e) {
      ServiceKey<T> key = ServiceKey.key(type, name);
      throw new RegistryException("Provisioning of `" + key + "` resulted in exception", e);
    }
  }

  @Override
  public @NonNull <T> T require(@NonNull ServiceKey<T> key) throws RegistryException {
    if (key.getName() == null) {
      return require(key.getType());
    } else {
      return require(key.getType(), key.getName());
    }
  }
}
