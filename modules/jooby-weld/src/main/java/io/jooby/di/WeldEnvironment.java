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

import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;
import io.jooby.Jooby;
import io.jooby.Reified;
import io.jooby.annotations.Path;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.literal.NamedLiteral;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;
import javax.inject.Inject;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class WeldEnvironment implements Extension {
  private final Jooby app;

  @Inject
  public WeldEnvironment(Jooby application) {
    this.app = application;
  }

  public void registerMvc(
      @Observes @WithAnnotations(Path.class) ProcessAnnotatedType<?> controller) {
    this.app.mvc(controller.getAnnotatedType().getJavaClass());
  }

  public void configureProperties(@Observes AfterBeanDiscovery beanDiscovery,
      BeanManager beanManager) {
    Config config = app.getEnvironment().getConfig();

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
      AnnotatedType<?> annotatedType = beanManager.createAnnotatedType(configClass);
      InjectionTarget<?> target = beanManager.createInjectionTarget(annotatedType);
      beanDiscovery.addBean()
          .addQualifier(literal)
          .addTypes(configType, Object.class)
          .name(configKey)
          .addInjectionPoints(target.getInjectionPoints())
          .createWith(c -> configValue);
    }
  }
}
