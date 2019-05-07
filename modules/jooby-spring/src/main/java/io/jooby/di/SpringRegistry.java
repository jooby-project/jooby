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

import io.jooby.Registry;
import io.jooby.RegistryException;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import javax.annotation.Nonnull;

public class SpringRegistry implements Registry {
  private final ApplicationContext ctx;

  public SpringRegistry(ApplicationContext ctx) {
    this.ctx = ctx;
  }

  @Nonnull @Override public <T> T require(@Nonnull Class<T> type) {
    try {
      return ctx.getBean(type);
    } catch (BeansException cause) {
      throw new RegistryException("Provisioning of `" + type.getName() + "` resulted in exception",
          cause);
    }
  }

  @Nonnull @Override public <T> T require(@Nonnull Class<T> type, @Nonnull String name) {
    try {
      return ctx.getBean(name, type);
    } catch (BeansException cause) {
      throw new RegistryException(
          "Provisioning of `" + type.getName() + "(" + name + ")" + "` resulted in exception", cause);
    }
  }
}
