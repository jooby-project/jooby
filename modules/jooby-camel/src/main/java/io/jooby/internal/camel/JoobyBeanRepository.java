/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.camel;

import static io.jooby.internal.camel.CamelBeans.camelBeanId;
import static java.util.Collections.emptySet;
import static java.util.Optional.ofNullable;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.camel.spi.BeanRepository;

import io.jooby.Jooby;
import io.jooby.ServiceKey;
import io.jooby.ServiceRegistry;
import jakarta.inject.Provider;

/** Look into {@link Jooby#getServices()} it doesn't extend lookup into DI. */
public class JoobyBeanRepository implements BeanRepository {

  private ServiceRegistry registry;

  public JoobyBeanRepository(ServiceRegistry registry) {
    this.registry = registry;
  }

  @Override
  public Object lookupByName(String name) {
    Provider provider = beanByName(name);
    return provider == null ? null : provider.get();
  }

  @Override
  public <T> T lookupByNameAndType(String name, Class<T> type) {
    return (T) getBean(ServiceKey.key(type, name));
  }

  @Override
  public <T> Map<String, T> findByTypeWithName(Class<T> type) {
    Object bean = getBean(ServiceKey.key(type));
    String beanName = null;
    if (bean == null) {
      // Look if there is named bean:
      Map.Entry<ServiceKey<?>, Provider<?>> def = beanByType(type);
      if (def != null) {
        bean = def.getValue().get();
        beanName = def.getKey().getName();
      }
    }
    return bean == null
        ? Collections.emptyMap()
        : (Map<String, T>)
            Collections.singletonMap(ofNullable(beanName).orElseGet(() -> camelBeanId(type)), bean);
  }

  @Override
  public <T> Set<T> findByType(Class<T> type) {
    return (Set<T>)
        ofNullable(getBean(ServiceKey.key(type))).map(Collections::singleton).orElse(emptySet());
  }

  private <T> Object getBean(ServiceKey<T> key) {
    return registry.getOrNull(key);
  }

  private Provider beanByName(String beanId) {
    var entry =
        findBean(
            key ->
                ofNullable(key.getName())
                    .orElseGet(() -> camelBeanId(key.getType()))
                    .equals(beanId));
    return entry == null ? null : entry.getValue();
  }

  private Map.Entry<ServiceKey<?>, Provider<?>> beanByType(Class type) {
    return findBean(key -> key.getType().equals(type));
  }

  private Map.Entry<ServiceKey<?>, Provider<?>> findBean(Predicate<ServiceKey<?>> predicate) {
    for (Map.Entry<ServiceKey<?>, Provider<?>> e : registry.entrySet()) {
      if (predicate.test(e.getKey())) {
        return e;
      }
    }
    return null;
  }
}
