package io.jooby.avaje.inject;

import java.util.NoSuchElementException;

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
  public <T> T require(Class<T> type) throws RegistryException {
    try {
      return beanScope.get(type);
    } catch (NoSuchElementException e) {
      ServiceKey<T> key = ServiceKey.key(type);
      throw new RegistryException("Provisioning of `" + key + "` resulted in exception", e);
    }
  }

  @Override
  public <T> T require(Class<T> type, String name) throws RegistryException {
    try {
      return beanScope.get(type, name);
    } catch (NoSuchElementException e) {
      ServiceKey<T> key = ServiceKey.key(type, name);
      throw new RegistryException("Provisioning of `" + key + "` resulted in exception", e);
    }
  }

  @Override
  public <T> T require(ServiceKey<T> key) throws RegistryException {
    return require(key.getType(), key.getName());
  }
}
