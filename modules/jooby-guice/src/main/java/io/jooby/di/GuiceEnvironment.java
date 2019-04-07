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

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.google.inject.util.Types;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;
import io.jooby.Environment;

import javax.annotation.Nonnull;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GuiceEnvironment implements Module {
  private Environment env;

  public GuiceEnvironment(@Nonnull Environment env) {
    this.env = env;
  }

  @Override public void configure(Binder binder) {
    Config config = env.getConfig();
    // terminal nodes
    for (Map.Entry<String, ConfigValue> entry : config.entrySet()) {
      String name = entry.getKey();
      Named named = Names.named(name);
      Object value = entry.getValue().unwrapped();
      if (value instanceof List) {
        List values = (List) value;
        Type listType = Types.listOf(values.iterator().next().getClass());
        Key key = Key.get(listType, Names.named(name));
        binder.bind(key).toInstance(values);
        value = values.stream().map(Object::toString).collect(Collectors.joining(","));
      }
      binder.bindConstant().annotatedWith(named).to(value.toString());
    }
    binder.bind(Config.class).toInstance(config);
    binder.bind(Environment.class).toInstance(env);
  }
}
