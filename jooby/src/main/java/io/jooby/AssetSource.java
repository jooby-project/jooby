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
import java.io.FileNotFoundException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * An asset source is a collection or provider of {@link Asset}. There are two implementations:
 *
 * <ul>
 *   <li>File system: using {@link #create(Path)}.</li>
 *   <li>Classpath/URL: using {@link #create(ClassLoader, String)}.</li>
 * </ul>
 */
public interface AssetSource {

  /**
   * Resolve an asset using the given path.
   *
   * @param path Path to look for.
   * @return An asset or <code>null</code>.
   */
  @Nullable Asset resolve(@Nonnull String path);

  /**
   * Classpath/url-based asset source. Useful for resolving files from classpath
   * (including jar files).
   *
   * @param loader Class loader.
   * @param location Classpath location.
   * @return An asset source.
   */
  static @Nonnull AssetSource create(@Nonnull ClassLoader loader, @Nonnull String location) {
    String safeloc = Router.normalizePath(location, true, true)
        .substring(1);
    String sep = safeloc.length() > 0 ? "/" : "";
    return path -> {
      String absolutePath = safeloc + sep + path;
      URL resource = loader.getResource(absolutePath);
      if (resource == null) {
        return null;
      }
      return Asset.create(absolutePath, resource);
    };
  }

  /**
   * Creates a source from given location. Assets are resolved from file system.
   *
   * @param location Asset directory.
   * @return A new file system asset source.
   */
  static @Nonnull AssetSource create(@Nonnull Path location) {
    Path absoluteLocation = location.toAbsolutePath();
    if (Files.isDirectory(absoluteLocation)) {
      return path -> {
        Path resource = absoluteLocation.resolve(path).normalize().toAbsolutePath();
        if (path.length() == 0) {
          resource = resource.resolve("index.html");
        }
        if (!Files.exists(resource)
            || Files.isDirectory(resource)
            || !resource.startsWith(absoluteLocation)) {
          return null;
        }
        return Asset.create(resource);
      };
    }
    if (Files.isRegularFile(location)) {
      Asset singleFile = Asset.create(absoluteLocation);
      return p -> singleFile;
    }
    throw Sneaky.propagate(new FileNotFoundException(location.toAbsolutePath().toString()));
  }
}
