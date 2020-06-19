/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.di;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;
import io.jooby.Environment;
import io.jooby.Jooby;
import io.jooby.Reified;
import io.jooby.ServiceKey;
import io.jooby.ServiceRegistry;
import io.jooby.annotations.Path;

import javax.annotation.Nonnull;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.literal.NamedLiteral;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;
import javax.enterprise.inject.spi.configurator.BeanConfigurator;
import javax.inject.Provider;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Weld extension. Exposes {@link Environment}, {@link Config} and application services into weld
 * container.
 *
 * Also, scan and register MVC routes annotated with {@link Path} annotation at top level class.
 *
 * @author edgar
 * @since 2.0.0
 */
public class JoobyExtension implements Extension {
  private final Jooby app;

  /**
   * Creates a new Jooby extension.
   *
   * @param application Application.
   */
  public JoobyExtension(@Nonnull Jooby application) {
    this.app = application;
  }

  /**
   * Register MVC routes annotated with {@link Path}.
   *
   * @param c Weld callback.
   */
  public void registerMvc(@Observes @WithAnnotations(Path.class) ProcessAnnotatedType<?> c) {
    this.app.mvc(c.getAnnotatedType().getJavaClass());
  }

  /**
   * Configure application services into a weld container.
   *
   * @param beanDiscovery Bean discovery.
   * @param beanManager Bean Manager.
   */
  public void configureServices(@Nonnull @Observes AfterBeanDiscovery beanDiscovery,
      @Nonnull BeanManager beanManager) {
    ServiceRegistry registry = app.getServices();
    Set<Map.Entry<ServiceKey<?>, Provider<?>>> entries = registry.entrySet();
    for (Map.Entry<ServiceKey<?>, Provider<?>> entry : entries) {
      registerSingleton(beanDiscovery, beanManager, entry.getKey().getType(),
          entry.getKey().getName(), entry.getValue());
    }
  }

  /**
   * Configure {@link Environment} and {@link Config} into a weld container.
   *
   * @param abd Bean discovery.
   * @param bm Bean Manager.
   */
  public void configureEnv(@Observes AfterBeanDiscovery abd, BeanManager bm) {
    Environment environment = app.getEnvironment();
    Config config = environment.getConfig();

    for (Map.Entry<String, ConfigValue> configEntry : config.entrySet()) {
      final String configKey = configEntry.getKey();
      final Object configValue = configEntry.getValue().unwrapped();
      Class configClass = configValue.getClass();
      Type configType = configClass;
      if (List.class.isAssignableFrom(configClass)) {
        List values = (List) configValue;
        configType = Reified.list(values.get(0).getClass()).getType();
      }
      if ("true".equals(configValue) || "false".equals(configValue)) {
        configClass = boolean.class;
        configType = boolean.class;
      }
      NamedLiteral literal = NamedLiteral.of(configKey);
      AnnotatedType<?> annotatedType = bm.createAnnotatedType(configClass);
      InjectionTarget<?> target = bm.createInjectionTarget(annotatedType);
      abd.addBean()
          .addQualifier(literal)
          .addTypes(configType, Object.class)
          .name(configKey)
          .addInjectionPoints(target.getInjectionPoints())
          .createWith(c -> configValue);
    }
  }

  private <T> void registerSingleton(AfterBeanDiscovery beanDiscovery, BeanManager beanManager,
      Class<T> type, String name, Object instance) {
    BeanConfigurator<Object> configurator = beanDiscovery.addBean();
    if (name != null) {
      configurator.addQualifier(NamedLiteral.of(name)).name(name);
    }
    if (type.isPrimitive() || type == String.class || Number.class.isAssignableFrom(type)) {
      AnnotatedType<?> annotatedType = beanManager.createAnnotatedType(type);
      InjectionTarget<?> target = beanManager.createInjectionTarget(annotatedType);
      configurator
          .addTypes(type)
          .addInjectionPoints(target.getInjectionPoints())
          .createWith(provider(instance));
    } else {
      configurator
          .addType(type)
          .scope(ApplicationScoped.class)
          .beanClass(type)
          .createWith(provider(instance));
    }
  }

  private static Function provider(Object instance) {
    if (instance instanceof Provider) {
      return c -> ((Provider) instance).get();
    }
    return c -> instance;
  }
}
