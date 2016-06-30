/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.hotreload;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;

import org.jboss.modules.DependencySpec;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.ModuleSpec;
import org.jboss.modules.ResourceLoaderSpec;
import org.jboss.modules.ResourceLoaders;

public class AppModuleLoader extends ModuleLoader {

  private Map<ModuleIdentifier, ModuleSpec> modules;

  public AppModuleLoader(final Map<ModuleIdentifier, ModuleSpec> modules) {
    this.modules = modules;
  }

  @Override
  protected ModuleSpec findModule(final ModuleIdentifier moduleId) throws ModuleLoadException {
    ModuleSpec mod = modules.get(moduleId);
    return mod == null ? super.findModule(moduleId) : mod;
  }

  public void unload(final Module module) {
    super.unloadModuleLocal(module);
  }

  /**
   * Build a flat jboss module, with some minor exceptions (like j2v8).
   *
   * @param name module name.
   * @param mainClass
   * @param cp
   * @return
   * @throws Exception
   */
  public static AppModuleLoader build(final String name, final String mainClass, final File... cp)
      throws Exception {
    Map<ModuleIdentifier, ModuleSpec> modules = newModule(name, mainClass, 0, "", cp);
    return new AppModuleLoader(modules);
  }

  private static Map<ModuleIdentifier, ModuleSpec> newModule(final String name,
      final String mainClass, final int level, final String prefix, final File... cp)
      throws Exception {
    Map<ModuleIdentifier, ModuleSpec> modules = new HashMap<>();

    String mId = name.replace(".jar", "");
    ModuleSpec.Builder builder = ModuleSpec.build(ModuleIdentifier.fromString(mId));

    int l = (prefix.length() + mId.length() + level);
    AppModule.debug("%1$" + l + "s", prefix + mId);
    for (File file : cp) {
      String fname = "└── " + file.getAbsolutePath();
      if (file.getName().startsWith("j2v8") && !name.equals(file.getName())) {
        ModuleSpec dependency = newModule(file.getName(), null, level + 2, "└── ", file)
            .values()
            .iterator()
            .next();
        builder.addDependency(
            DependencySpec.createModuleDependencySpec(dependency.getModuleIdentifier()));
        modules.put(dependency.getModuleIdentifier(), dependency);
      } else {
        AppModule.debug("%1$" + (fname.length() + level + 2) + "s", fname);
        if (file.getName().endsWith(".jar")) {
          builder.addResourceRoot(ResourceLoaderSpec
              .createResourceLoaderSpec(ResourceLoaders
                  .createJarResourceLoader(file.getName(), new JarFile(file))));
        } else {
          builder.addResourceRoot(ResourceLoaderSpec
              .createResourceLoaderSpec(ResourceLoaders
                  .createFileResourceLoader(file.getName(), file)));
        }
      }
    }
    Set<String> sysPaths = sysPaths();

    AppModule.trace("system packages:");
    sysPaths.forEach(p -> AppModule.trace("  %s", p));

    builder.addDependency(DependencySpec.createSystemDependencySpec(sysPaths));
    builder.addDependency(DependencySpec.createLocalDependencySpec());

    if (mainClass != null) {
      builder.setMainClass(mainClass);
    }

    ModuleSpec module = builder.create();
    modules.put(module.getModuleIdentifier(), builder.create());
    return modules;
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  private static Set<String> jdkPaths() throws Exception {
    Class jdkPath = AppModuleLoader.class.getClassLoader().loadClass("org.jboss.modules.JDKPaths");
    Field field = jdkPath.getDeclaredField("JDK");
    field.setAccessible(true);
    return (Set<String>) field.get(null);
  }

  private static Set<String> sysPaths() throws Exception {
    Set<String> pkgs = new LinkedHashSet<>();

    pkgs.addAll(jdkPaths());
    pkgs.addAll(pkgs(new InputStreamReader(AppModule.class.getResourceAsStream("pkgs"))));

    /**
     * Hack to let users to configure system packages, javax.transaction cause issues with
     * hibernate.
     */
    pkgs.addAll(pkgs(Paths.get("src", "etc", "jboss-modules", "pkgs.includes").toFile()));

    pkgs.removeAll(pkgs(Paths.get("src", "etc", "jboss-modules", "pkgs.excludes").toFile()));
    return pkgs;
  }

  private static Set<String> pkgs(final File file) throws IOException {
    if (file.exists()) {
      return pkgs(new FileReader(file));
    }
    return new LinkedHashSet<String>();
  }

  private static Set<String> pkgs(final Reader reader) throws IOException {
    try (BufferedReader in = new BufferedReader(reader)) {
      Set<String> pkgs = new LinkedHashSet<String>();
      String line = in.readLine();
      while (line != null) {
        pkgs.add(line.trim());
        line = in.readLine();
      }
      return pkgs;
    }
  }

}
