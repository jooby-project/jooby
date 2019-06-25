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

public class SpringModule implements Extension {

  private AnnotationConfigApplicationContext applicationContext;

  private boolean registerMvcRoutes = true;

  private boolean refresh = true;

  private String[] packages;

  public SpringModule(@Nonnull AnnotationConfigApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  public SpringModule() {
    this.applicationContext = null;
  }

  public SpringModule(String... packages) {
    this.applicationContext = null;
    this.packages = packages;
  }

  public SpringModule noRefresh() {
    this.refresh = false;
    return this;
  }

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

      applicationContext = defaultApplicationContext(packages);

      ConfigurableEnvironment configurableEnvironment = applicationContext.getEnvironment();
      String[] profiles = environment.getActiveNames().toArray(new String[0]);
      configurableEnvironment.setActiveProfiles(profiles);
      configurableEnvironment.setDefaultProfiles(profiles);

      Config config = environment.getConfig();
      MutablePropertySources propertySources = configurableEnvironment.getPropertySources();
      propertySources.addFirst(new ConfigPropertySource("application", config));

      ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();
      beanFactory.registerSingleton("conf", config);
      beanFactory.registerSingleton("env", environment);
      beanFactory.registerSingleton("environment", configurableEnvironment);

      // Add services:
      ServiceRegistry registry = application.getServices();
      for (Map.Entry<ServiceKey<?>, Provider<?>> entry : registry.entrySet()) {
        ServiceKey key = entry.getKey();
        Provider provider = entry.getValue();
        applicationContext.registerBean(key.getName(), key.getType(), () -> provider.get());
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

  public static AnnotationConfigApplicationContext defaultApplicationContext(
      @Nonnull String... packages) {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.scan(packages);
    return context;
  }
}
