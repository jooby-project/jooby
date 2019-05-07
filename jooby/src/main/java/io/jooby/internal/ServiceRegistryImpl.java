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
package io.jooby.internal;

import io.jooby.ServiceKey;
import io.jooby.ServiceRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ServiceRegistryImpl implements ServiceRegistry {

  private Map<ServiceKey<?>, Object> registry = new ConcurrentHashMap<>();

  @Nonnull @Override public Set<ServiceKey<?>> keySet() {
    return registry.keySet();
  }

  @Nullable @Override public <T> T getOrNull(@Nonnull ServiceKey<T> key) {
    return (T) registry.get(key);
  }

  @Nullable @Override public <T> T put(@Nonnull ServiceKey<T> key, T service) {
    return (T) registry.put(key, service);
  }

  @Nullable @Override public <T> T putIfAbsent(@Nonnull ServiceKey<T> type, T service) {
    return (T) registry.putIfAbsent(type, service);
  }
}
