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
package io.jooby;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Utility class to access application services.
 *
 * @param <T> Service type.
 */
public final class ServiceKey<T> {
  private final Class<T> type;

  private final int hashCode;

  private final String name;

  private ServiceKey(Class<T> type, String name) {
    this.type = type;
    this.name = name;
    this.hashCode = Objects.hash(type, name);
  }

  /**
   * Resource type.
   *
   * @return Resource type.
   */
  public @Nonnull Class<T> getType() {
    return type;
  }

  /**
   * Resource name or <code>null</code>.
   *
   * @return Resource name or <code>null</code>.
   */
  public @Nullable String getName() {
    return name;
  }

  @Override public boolean equals(Object obj) {
    if (obj instanceof ServiceKey) {
      ServiceKey that = (ServiceKey) obj;
      return this.type == that.type && Objects.equals(this.name, that.name);
    }
    return false;
  }

  @Override public int hashCode() {
    return hashCode;
  }

  @Override public String toString() {
    if (name == null) {
      return type.getName();
    }
    return type.getName() + "(" + name + ")";
  }

  /**
   * Creates a resource key.
   *
   * @param type Resource type.
   * @param <T> Type.
   * @return A new resource key.
   */
  public static @Nonnull  <T> ServiceKey<T> key(@Nonnull Class<T> type) {
    return new ServiceKey<>(type, null);
  }

  /**
   * Creates a named resource key.
   *
   * @param type Resource type.
   * @param name Resource name.
   * @param <T> Type.
   * @return A new resource key.
   */
  public static @Nonnull  <T> ServiceKey<T> key(@Nonnull Class<T> type, @Nonnull String name) {
    return new ServiceKey<>(type, name);
  }
}
