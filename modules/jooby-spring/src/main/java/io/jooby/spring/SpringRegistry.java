/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.spring;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Registry;
import io.jooby.ServiceKey;
import io.jooby.exception.RegistryException;

class SpringRegistry implements Registry {
  private final ApplicationContext ctx;

  SpringRegistry(ApplicationContext ctx) {
    this.ctx = ctx;
  }

  @NonNull @Override
  public <T> T require(@NonNull Class<T> type) {
    try {
      return ctx.getBean(type);
    } catch (BeansException cause) {
      throw new RegistryException(
          "Provisioning of `" + type.getName() + "` resulted in exception", cause);
    }
  }

  @NonNull @Override
  public <T> T require(@NonNull Class<T> type, @NonNull String name) {
    try {
      return ctx.getBean(name, type);
    } catch (BeansException cause) {
      throw new RegistryException(
          "Provisioning of `" + type.getName() + "(" + name + ")" + "` resulted in exception",
          cause);
    }
  }

  @NonNull @Override
  public <T> T require(@NonNull ServiceKey<T> key) throws RegistryException {
    String name = key.getName();
    return name == null ? require(key.getType()) : require(key.getType(), name);
  }
}
