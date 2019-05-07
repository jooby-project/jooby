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

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.google.inject.util.Types;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;
import io.jooby.Environment;
import io.jooby.Jooby;
import io.jooby.ServiceKey;
import io.jooby.ServiceRegistry;

import javax.annotation.Nonnull;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JoobyModule extends AbstractModule {
  private Jooby application;

  public JoobyModule(@Nonnull Jooby application) {
    this.application = application;
  }

  @Override protected void configure() {
    configureEnv(application.getEnvironment());
    configureResources(application.getServices());
  }

  private void configureResources(ServiceRegistry registry) {
    // bind the available resources as well, supporting the name annotations that may be set
    for (ServiceKey key: registry.keySet()) {
      Object instance = registry.get(key);
      if (key.getName() != null) {
        bind(key.getType()).annotatedWith(Names.named(key.getName())).toInstance(instance);
      } else {
        bind(key.getType()).toInstance(instance);
      }
    }
  }

  private void configureEnv(Environment env) {
    Config config = env.getConfig();
    bind(Config.class).toInstance(config);
    bind(Environment.class).toInstance(env);

    // configuration properties
    for (Map.Entry<String, ConfigValue> entry : config.entrySet()) {
      String name = entry.getKey();
      Named named = Names.named(name);
      Object value = entry.getValue().unwrapped();
      if (value instanceof List) {
        List values = (List) value;
        Type listType = Types.listOf(values.iterator().next().getClass());
        Key key = Key.get(listType, Names.named(name));
        bind(key).toInstance(values);
        value = values.stream().map(Object::toString).collect(Collectors.joining(","));
      }
      bindConstant().annotatedWith(named).to(value.toString());
    }
  }
}
