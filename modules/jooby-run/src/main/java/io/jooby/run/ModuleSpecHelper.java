/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.run;

import static org.jboss.modules.ResourceLoaderSpec.createResourceLoaderSpec;
import static org.jboss.modules.ResourceLoaders.createJarResourceLoader;
import static org.jboss.modules.ResourceLoaders.createPathResourceLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.jar.JarFile;

import org.jboss.modules.DependencySpec;
import org.jboss.modules.ModuleDependencySpecBuilder;
import org.jboss.modules.ModuleSpec;
import org.jboss.modules.PathUtils;
import org.jboss.modules.ResourceLoaderSpec;
import org.jboss.modules.filter.PathFilters;

final class ModuleSpecHelper {

  private ModuleSpecHelper() {}

  public static ModuleSpec create(String name, Set<Path> resources, Set<String> dependencies) {
    ModuleSpec.Builder builder = newModule(name, resources);

    // dependencies
    for (String dependency : dependencies) {
      builder.addDependency(
          new ModuleDependencySpecBuilder()
              .setImportFilter(PathFilters.acceptAll())
              .setExportFilter(PathFilters.getMetaInfServicesFilter())
              .setName(dependency)
              .setOptional(false)
              .build());
    }
    return builder.create();
  }

  private static ModuleSpec.Builder newModule(String name, Set<Path> resources) {
    try {
      ModuleSpec.Builder builder = ModuleSpec.build(name);
      // Add all JDK classes
      builder.addDependency(DependencySpec.createSystemDependencySpec(PathUtils.getPathSet(null)));
      // needed, so that the module can load classes from the resource root
      builder.addDependency(DependencySpec.createLocalDependencySpec());
      // Add the module's own content
      builder.addDependency(DependencySpec.OWN_DEPENDENCY);

      for (Path path : resources) {
        if (Files.isDirectory(path)) {
          builder.addResourceRoot(
              ResourceLoaderSpec.createResourceLoaderSpec(createPathResourceLoader(path)));
        } else {
          builder.addResourceRoot(
              createResourceLoaderSpec(createJarResourceLoader(new JarFile(path.toFile()))));
        }
      }
      return builder;
    } catch (IOException x) {
      throw JoobyRun.sneakyThrow0(x);
    }
  }
}
