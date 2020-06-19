/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.di;

import com.typesafe.config.Config;
import io.jooby.Environment;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.ServiceKey;
import io.jooby.ServiceRegistry;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.stereotype.Controller;

import javax.annotation.Nonnull;
import javax.inject.Provider;
import java.util.Map;

/**
 * Spring module: https://jooby.io/modules/spring.
 *
 * Jooby integrates the {@link io.jooby.ServiceRegistry} into the Spring Core framework.
 *
 * Usage:
 *
 * <pre>{@code
 * {
 *
 *
 *   install(new SpringModule());
 *
 * }
 *
 * }</pre>
 *
 * Require calls are going to be resolve by Spring now.
 *
 * Spring scan the {@link Jooby#getBasePackage()}, unless you specify them explicitly.
 *
 * @author edgar
 * @since 2.0.0
 */
public class SpringModule implements Extension {

  private AnnotationConfigApplicationContext applicationContext;

  private boolean registerMvcRoutes = true;

  private boolean refresh = true;

  private String[] packages;

  /**
   * Creates a new Spring module using the given application context.
   *
   * @param applicationContext Application context to use.
   */
  public SpringModule(@Nonnull AnnotationConfigApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  /**
   * Creates a new Spring module, scan the default package: {@link Jooby#getBasePackage()}.
   */
  public SpringModule() {
  }

  /**
   * Creates a new Spring module and scan the provided packages.
   *
   * @param packages Package to scan.
   */
  public SpringModule(@Nonnull String... packages) {
    this.packages = packages;
  }

  /**
   * Indicates the Spring application context should NOT be refreshed. Default is: true.
   *
   * @return This module.
   */
  public SpringModule noRefresh() {
    this.refresh = false;
    return this;
  }

  /**
   * Turn off discovering/scanning of MVC routes. For Spring integration an MVC route must be
   * annotated with {@link Controller}.
   *
   * @return This module.
   */
  public SpringModule noMvcRoutes() {
    this.registerMvcRoutes = false;
    return this;
  }

  @Override public boolean lateinit() {
    return true;
  }

  @Override public void install(@Nonnull Jooby application) throws Exception {
    if (applicationContext == null) {
      String[] packages = this.packages;
      if (packages == null) {
        String basePackage = application.getBasePackage();
        if (basePackage == null) {
          throw new IllegalArgumentException(
              "SpringModule application context requires at least one package to scan.");
        }
        packages = new String[]{basePackage};
      }
      Environment environment = application.getEnvironment();

      applicationContext = new AnnotationConfigApplicationContext();
      applicationContext.scan(packages);

      ConfigurableEnvironment configurableEnvironment = applicationContext.getEnvironment();
      String[] profiles = environment.getActiveNames().toArray(new String[0]);
      configurableEnvironment.setActiveProfiles(profiles);
      configurableEnvironment.setDefaultProfiles(profiles);

      Config config = environment.getConfig();
      MutablePropertySources propertySources = configurableEnvironment.getPropertySources();
      propertySources.addFirst(new ConfigPropertySource(config));

      ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();
      beanFactory.registerSingleton("conf", config);
      beanFactory.registerSingleton("env", environment);
      beanFactory.registerSingleton("environment", configurableEnvironment);

      // Add services:
      ServiceRegistry registry = application.getServices();
      for (Map.Entry<ServiceKey<?>, Provider<?>> entry : registry.entrySet()) {
        ServiceKey key = entry.getKey();
        if (!ignoreEntry(key.getType())) {
          Provider provider = entry.getValue();
          applicationContext.registerBean(key.getName(), key.getType(), () -> provider.get());
        }
      }

      application.onStop(applicationContext);
    }
    if (refresh) {
      // fire refresh
      applicationContext.refresh();
    }

    application.registry(new SpringRegistry(applicationContext));

    if (registerMvcRoutes) {
      String[] names = applicationContext
          .getBeanNamesForAnnotation(Controller.class);
      ClassLoader loader = application.getClass().getClassLoader();
      for (String name : names) {
        BeanDefinition bean = applicationContext.getBeanDefinition(name);
        Class mvcClass = loader.loadClass(bean.getBeanClassName());
        application.mvc(mvcClass);
      }
    }
  }

  private boolean ignoreEntry(Class type) {
    return type == Environment.class || type == Config.class;
  }
}
