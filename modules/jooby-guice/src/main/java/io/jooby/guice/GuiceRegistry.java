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
import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Registry;
import io.jooby.ServiceKey;
import io.jooby.exception.RegistryException;

class GuiceRegistry implements Registry {
  private final Injector injector;

  GuiceRegistry(Injector injector) {
    this.injector = injector;
  }

  @NonNull @Override
  public <T> T require(@NonNull Class<T> type) {
    return require(Key.get(type));
  }

  @NonNull @Override
  public <T> T require(@NonNull Class<T> type, @NonNull String name) {
    return require(Key.get(type, Names.named(name)));
  }

  @NonNull @Override
  public <T> T require(@NonNull ServiceKey<T> key) throws RegistryException {
    String name = key.getName();
    return name == null ? require(key.getType()) : require(key.getType(), name);
  }

  @NonNull private <T> T require(@NonNull Key<T> key) {
    try {
      return injector.getInstance(key);
    } catch (ProvisionException | ConfigurationException x) {
      throw new RegistryException("Provisioning of `" + key + "` resulted in exception", x);
    }
  }
}
