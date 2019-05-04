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
package io.jooby.run;

import org.jboss.modules.ModuleFinder;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.ModuleSpec;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

class FlattenClasspath implements ModuleFinder {

  private final Set<Path> resources;
  private final String name;

  public FlattenClasspath(String name, Set<Path> resources, Set<Path> dependencies) {
    this.name = name;
    this.resources = new LinkedHashSet<>(resources.size() + dependencies.size());
    this.resources.addAll(resources);
    this.resources.addAll(dependencies);
  }

  @Override public ModuleSpec findModule(String name, ModuleLoader delegateLoader)
      throws ModuleLoadException {
    if (this.name.equals(name)) {
      return Specs.spec(name, resources, Collections.emptySet());
    }

    return null;
  }
}
