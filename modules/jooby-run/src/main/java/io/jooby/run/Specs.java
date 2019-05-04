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

import org.jboss.modules.DependencySpec;
import org.jboss.modules.ModuleDependencySpecBuilder;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleSpec;
import org.jboss.modules.PathUtils;
import org.jboss.modules.ResourceLoaderSpec;
import org.jboss.modules.filter.PathFilters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarFile;

import static org.jboss.modules.ResourceLoaderSpec.createResourceLoaderSpec;
import static org.jboss.modules.ResourceLoaders.createJarResourceLoader;
import static org.jboss.modules.ResourceLoaders.createPathResourceLoader;

public class Specs {

  static DependencySpec metaInf(String moduleName) {
    return new ModuleDependencySpecBuilder()
        .setImportFilter(PathFilters.acceptAll())
        .setExportFilter(PathFilters.getMetaInfServicesFilter())
        .setName(moduleName)
        .setOptional(false)
        .build();
  }

  public static ModuleSpec spec(String name, Set<Path> resources, Set<String> dependencies) throws ModuleLoadException {
    try {
      ModuleSpec.Builder bld = ModuleSpec.build(name);
      for (Path path : new LinkedHashSet<>(resources)) {
        if (Files.isDirectory(path)) {
          bld.addResourceRoot(ResourceLoaderSpec
              .createResourceLoaderSpec(createPathResourceLoader(path)));
        } else {
          bld.addResourceRoot(
              createResourceLoaderSpec(createJarResourceLoader(new JarFile(path.toFile()))));
        }
      }

      //needed, so that the module can load classes from the resource root
      bld.addDependency(DependencySpec.createLocalDependencySpec());
      //add dependency on the JDK paths
      bld.addDependency(DependencySpec.createSystemDependencySpec(PathUtils.getPathSet(null)));
      // dependencies
      for (String dependency : new LinkedHashSet<>(dependencies)) {
        bld.addDependency(Specs.metaInf(dependency));
      }
      return bld.create();
    } catch (IOException x) {
      throw new ModuleLoadException(name, x);
    }
  }
}
