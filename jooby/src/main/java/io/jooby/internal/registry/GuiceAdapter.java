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
package io.jooby.internal.registry;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import io.jooby.Registry;

import javax.annotation.Nonnull;

public class GuiceAdapter implements Registry {
  private final Injector injector;

  public GuiceAdapter(Object candidate) {
    this.injector = (Injector) candidate;
  }

  @Nonnull @Override public <T> T require(@Nonnull Class<T> type) {
    return injector.getInstance(type);
  }

  @Nonnull @Override public <T> T require(@Nonnull Class<T> type, @Nonnull String name) {
    return injector.getInstance(Key.get(type, Names.named(name)));
  }
}
