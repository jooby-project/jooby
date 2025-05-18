/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.avaje.inject;

import java.lang.reflect.Type;
import java.util.NoSuchElementException;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
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
  public @NonNull <T> T require(@NonNull Reified<T> type, @NonNull String name)
      throws RegistryException {
    return getBean(type.getType(), name);
  }

  @NonNull @Override
  public <T> T require(@NonNull Reified<T> type) throws RegistryException {
    return getBean(type.getType(), null);
  }

  @Override
  public @NonNull <T> T require(@NonNull Class<T> type) throws RegistryException {
    return getBean(type, null);
  }

  @Override
  public @NonNull <T> T require(@NonNull Class<T> type, @NonNull String name)
      throws RegistryException {
    return getBean(type, name);
  }

  @Override
  public @NonNull <T> T require(@NonNull ServiceKey<T> key) throws RegistryException {
    return getBean(key.getType(), key.getName());
  }

  private @NonNull <T> T getBean(@NonNull Type type, @Nullable String name)
      throws RegistryException {
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
