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

public class AttributeKey<T> {

  private final Class<T> type;
  private final String name;
  private final int hashCode;

  public AttributeKey(@Nonnull Class<T> type, @Nonnull String name) {
    this.type = type;
    this.name = name;
    this.hashCode = type.hashCode() * 31 + name.hashCode();
  }

  public AttributeKey(@Nonnull Class<T> type) {
    this.type = type;
    this.name = null;
    this.hashCode = type.hashCode();
  }

  public @Nullable String getName() {
    return name;
  }

  public @Nonnull Class<T> getType() {
    return type;
  }

  @Override public int hashCode() {
    return hashCode;
  }

  @Override public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof AttributeKey) {
      AttributeKey that = (AttributeKey) obj;
      return Objects.equals(name, that.name) && type.equals(that.type);
    }
    return false;
  }

  @Override public String toString() {
    StringBuilder string = new StringBuilder();
    string.append(type.getTypeName());
    if (name != null) {
      string.append(".").append(name);
    }
    return string.toString();
  }
}
