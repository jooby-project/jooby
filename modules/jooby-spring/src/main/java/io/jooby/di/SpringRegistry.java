/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.di;

import io.jooby.Registry;
import io.jooby.RegistryException;
import io.jooby.ServiceKey;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import javax.annotation.Nonnull;

public class SpringRegistry implements Registry {
  private final ApplicationContext ctx;

  public SpringRegistry(ApplicationContext ctx) {
    this.ctx = ctx;
  }

  @Nonnull @Override public <T> T require(@Nonnull Class<T> type) {
    try {
      return ctx.getBean(type);
    } catch (BeansException cause) {
      throw new RegistryException("Provisioning of `" + type.getName() + "` resulted in exception",
          cause);
    }
  }

  @Nonnull @Override public <T> T require(@Nonnull Class<T> type, @Nonnull String name) {
    try {
      return ctx.getBean(name, type);
    } catch (BeansException cause) {
      throw new RegistryException(
          "Provisioning of `" + type.getName() + "(" + name + ")" + "` resulted in exception",
          cause);
    }
  }

  @Nonnull @Override public <T> T require(@Nonnull ServiceKey<T> key) throws RegistryException {
    String name = key.getName();
    return name == null ? require(key.getType()) : require(key.getType(), name);
  }
}
