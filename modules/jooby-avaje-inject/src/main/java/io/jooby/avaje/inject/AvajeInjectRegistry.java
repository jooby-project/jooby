/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.avaje.inject;

import java.lang.reflect.Type;
import java.util.NoSuchElementException;

import org.jspecify.annotations.Nullable;

import io.avaje.inject.BeanScope;
import io.jooby.Registry;
import io.jooby.Reified;
import io.jooby.ServiceKey;
import io.jooby.exception.RegistryException;

class AvajeInjectRegistry implements Registry {

  private final BeanScope beanScope;

  public AvajeInjectRegistry(BeanScope beanScope) {
    this.beanScope = beanScope;
  }

  @Override
  public <T> T require(Reified<T> type, String name) throws RegistryException {
    return getBean(type.getType(), name);
  }

  @Override
  public <T> T require(Reified<T> type) throws RegistryException {
    return getBean(type.getType(), null);
  }

  @Override
  public <T> T require(Class<T> type) throws RegistryException {
    return getBean(type, null);
  }

  @Override
  public <T> T require(Class<T> type, String name) throws RegistryException {
    return getBean(type, name);
  }

  @Override
  public <T> T require(ServiceKey<T> key) throws RegistryException {
    return getBean(key.getType(), key.getName());
  }

  private <T> T getBean(Type type, @Nullable String name) throws RegistryException {
    try {
      return name == null ? beanScope.get(type) : beanScope.get(type, name);
    } catch (NoSuchElementException e) {
      var key =
          name == null
              ? ServiceKey.key(Reified.get(type))
              : ServiceKey.key(Reified.get(type), name);
      throw new RegistryException("Provisioning of `" + key + "` resulted in exception", e);
    }
  }
}
