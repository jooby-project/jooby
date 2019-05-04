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
import java.util.Set;

public class ProjectClasspath implements ModuleFinder {

  private static final String DEPS = "dependencies";

  private String name;

  private Set<Path> resources;

  private Set<Path> dependencies;

  public ProjectClasspath(String name, Set<Path> resources, Set<Path> dependencies) {
    this.name = name;
    this.resources = resources;
    this.dependencies = dependencies;
  }

  @Override public ModuleSpec findModule(String name, ModuleLoader delegateLoader)
      throws ModuleLoadException {
    if (this.name.equals(name)) {
      // Main project
      return Specs.spec(name, resources, Collections.singleton(DEPS));
    }
    if (DEPS.equals(name)) {
      return Specs.spec(name, dependencies, Collections.emptySet());
    }
    return null;
  }
}
