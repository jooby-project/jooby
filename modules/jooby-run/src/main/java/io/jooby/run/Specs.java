/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
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
import java.util.Set;
import java.util.jar.JarFile;

import static org.jboss.modules.ResourceLoaderSpec.createResourceLoaderSpec;
import static org.jboss.modules.ResourceLoaders.createJarResourceLoader;
import static org.jboss.modules.ResourceLoaders.createPathResourceLoader;

class Specs {

  static DependencySpec metaInf(String moduleName) {
    return new ModuleDependencySpecBuilder()
        .setImportFilter(PathFilters.acceptAll())
        .setExportFilter(PathFilters.getMetaInfServicesFilter())
        .setName(moduleName)
        .setOptional(false)
        .build();
  }

  public static ModuleSpec spec(String name, Set<Path> resources, Set<String> dependencies)
      throws ModuleLoadException {
    try {
      ModuleSpec.Builder builder = ModuleSpec.build(name);
      for (Path path : resources) {
        if (Files.isDirectory(path)) {
          builder.addResourceRoot(ResourceLoaderSpec
              .createResourceLoaderSpec(createPathResourceLoader(path)));
        } else {
          builder.addResourceRoot(
              createResourceLoaderSpec(createJarResourceLoader(new JarFile(path.toFile()))));
        }
      }

      //needed, so that the module can load classes from the resource root
      builder.addDependency(DependencySpec.createLocalDependencySpec());
      //add dependency on the JDK paths
      builder.addDependency(DependencySpec.createSystemDependencySpec(PathUtils.getPathSet(null)));
      // dependencies
      for (String dependency : dependencies) {
        builder.addDependency(Specs.metaInf(dependency));
      }
      return builder.create();
    } catch (IOException x) {
      throw new ModuleLoadException(name, x);
    }
  }
}
