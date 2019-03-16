/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright 2014 Edgar Espina
 */
package io.jooby.di;

import io.jooby.Extension;
import io.jooby.Jooby;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Controller;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Spring implements Extension {

  private AnnotationConfigApplicationContext applicationContext;

  private boolean registerMvcRoutes = true;

  private boolean refresh = true;

  private String[] packages;

  public Spring(@Nonnull AnnotationConfigApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  public Spring() {
    this.applicationContext = null;
  }

  public Spring(String... packages) {
    this.applicationContext = null;
    this.packages = packages;
  }

  public Spring noRefresh() {
    this.refresh = false;
    return this;
  }

  public Spring noMvcRoutes() {
    this.registerMvcRoutes = false;
    return this;
  }

  @Override public void install(@Nonnull Jooby application) throws Exception {
    if (applicationContext == null) {
      String[] packages = this.packages;
      if (packages == null) {
        String basePackage = application.basePackage();
        if (basePackage == null) {
          throw new IllegalArgumentException(
              "Spring application context requires at least one package to scan.");
        }
        packages = new String[]{basePackage};
      }
      applicationContext = defaultApplicationContext(packages);

      application.onStop(applicationContext);
    }
    if (refresh) {
      // fire refresh
      applicationContext.refresh();
    }

    application.registry(new SpringRegistry(applicationContext));

    if (registerMvcRoutes) {
      Set<String> names = Stream.concat(
          Stream.of(applicationContext
              .getBeanNamesForAnnotation(io.jooby.annotations.Controller.class)),
          Stream.of(applicationContext
              .getBeanNamesForAnnotation(Controller.class)))
          .collect(Collectors.toSet());
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
