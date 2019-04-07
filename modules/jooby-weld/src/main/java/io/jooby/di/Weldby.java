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

import io.jooby.Environment;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.annotations.Controller;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;

import javax.annotation.Nonnull;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.util.AnnotationLiteral;

import static org.jboss.weld.environment.se.Weld.DEV_MODE_SYSTEM_PROPERTY;
import static org.jboss.weld.environment.se.Weld.SHUTDOWN_HOOK_SYSTEM_PROPERTY;

public class Weldby implements Extension {

  private static final AnnotationLiteral<Controller> CONTROLLER = new AnnotationLiteral<Controller>() {
  };

  private WeldContainer container;

  private String[] packages;

  public Weldby(@Nonnull WeldContainer container) {
    this.container = container;
  }

  public Weldby(@Nonnull String... packages) {
    this.packages = packages;
    this.container = null;
  }

  @Override public void install(@Nonnull Jooby application) {
    if (container == null) {
      if (packages == null || packages.length == 0) {
        String basePackage = application.getBasePackage();
        if (basePackage == null) {
          throw new IllegalStateException("Weld requires at least one package to scan.");
        }
        packages = new String[]{basePackage};
      }
      Environment environment = application.getEnvironment();
      Weld weld = new Weld()
          .disableDiscovery()
          .addPackages(true, toPackages(packages))
          .addProperty(SHUTDOWN_HOOK_SYSTEM_PROPERTY, false)
          .addExtension(new WeldEnvironment(environment));

      application.onStop(weld::shutdown);

      container = weld.initialize();
    }

    BeanManager beanManager = container.getBeanManager();
    for (Bean<?> bean : beanManager.getBeans(Object.class, CONTROLLER)) {
      application.mvc(bean.getBeanClass());
    }

    application.registry(new WeldRegistry(container));
  }

  private static Package[] toPackages(String[] packages) {
    Package[] result = new Package[packages.length];
    for (int i = 0; i < packages.length; i++) {
      result[i] = Package.getPackage(packages[i]);
    }
    return result;
  }
}
