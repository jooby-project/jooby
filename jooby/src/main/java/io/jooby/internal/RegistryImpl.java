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

import io.jooby.Registry;
import io.jooby.internal.registry.GuiceAdapter;
import io.jooby.internal.registry.SpringAdapter;
import io.jooby.internal.registry.WeldAdapter;

public class RegistryImpl {
  public static Registry wrap(ClassLoader loader, Object candidate) {
    if (candidate instanceof Registry) {
      return (Registry) candidate;
    }
    if (isInstanceOf(loader, "com.google.inject.Injector", candidate)) {
      return new GuiceAdapter(candidate);
    }
    if (isInstanceOf(loader, "org.jboss.weld.environment.se.WeldContainer", candidate)) {
      return new WeldAdapter(candidate);
    }
    if (isInstanceOf(loader, "org.springframework.context.ApplicationContext", candidate)) {
      return new SpringAdapter(candidate);
    }
    throw new IllegalArgumentException("No registry adapter for: " + candidate);
  }

  private static boolean isInstanceOf(ClassLoader loader, String klass, Object candidate) {
    try {
      return loader.loadClass(klass).isInstance(candidate);
    } catch (ClassNotFoundException x) {
      return false;
    }
  }
}
