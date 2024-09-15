/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.impl.engine.DefaultInjector;
import org.apache.camel.spi.CamelBeanPostProcessor;
import org.apache.camel.spi.Injector;
import org.apache.camel.support.PluginHelper;

import io.jooby.Registry;
import io.jooby.ServiceKey;
import io.jooby.SneakyThrows;
import io.jooby.exception.RegistryException;

public class JoobyInjector implements Injector {
  private final DefaultInjector defaultInjector;
  private final Registry registry;
  private final CamelContext camel;
  private final CamelBeanPostProcessor postProcessor;

  public JoobyInjector(CamelContext camel, Registry registry) {
    this.registry = registry;
    this.camel = camel;
    this.postProcessor = PluginHelper.getBeanPostProcessor(camel);
    this.defaultInjector = new DefaultInjector(camel);
  }

  @Override
  public <T> T newInstance(Class<T> type) {
    return newInstance(type, true);
  }

  @Override
  public <T> T newInstance(Class<T> type, String factoryMethod) {
    // fallback to default injector
    return defaultInjector.newInstance(type, factoryMethod);
  }

  @Override
  public <T> T newInstance(Class<T> type, Class<?> factoryClass, String factoryMethod) {
    return defaultInjector.newInstance(type, factoryClass, factoryMethod);
  }

  @Override
  public <T> T newInstance(Class<T> type, boolean postProcessBean) {
    T instance = require(ServiceKey.key(type));
    if (instance != null) {
      return postProcessBean(instance, postProcessBean);
    }
    return defaultInjector.newInstance(type, postProcessBean);
  }

  private <T> T require(ServiceKey<T> key) {
    try {
      return registry.require(key);
    } catch (RegistryException notfound) {
      return null;
    }
  }

  private <T> T postProcessBean(T bean, boolean postProcessBean) {
    CamelContextAware.trySetCamelContext(bean, camel);
    if (postProcessBean) {
      try {
        this.postProcessor.postProcessBeforeInitialization(bean, bean.getClass().getName());
        this.postProcessor.postProcessAfterInitialization(bean, bean.getClass().getName());
      } catch (Exception cause) {
        throw SneakyThrows.propagate(cause);
      }
    }
    return bean;
  }

  @Override
  public boolean supportsAutoWiring() {
    return true;
  }
}
