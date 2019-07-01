/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import io.jooby.internal.FileDiskAssetSource;
import io.jooby.internal.FolderDiskAssetSource;

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
    String safeloc = Router.normalizePath(location, false, true)
        .substring(1);
    MediaType type = MediaType.byFile(location);
    if (type != MediaType.octetStream) {
      URL resource = loader
          .getResource(location.startsWith("/") ? location.substring(1) : location);
      if (resource != null) {
        return path -> Asset.create(location, resource);
      }
    }
    String prefix = safeloc + (safeloc.length() > 0 ? "/" : "");
    return path -> {
      String[] paths = {prefix + path + "/index.html", prefix + path};
      for (String it : paths) {
        URL resource = loader.getResource(it);
        if (resource != null) {
          return Asset.create(it, resource);
        }
      }
      return null;
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
      return new FolderDiskAssetSource(absoluteLocation);
    } else if (Files.isRegularFile(location)) {
      return new FileDiskAssetSource(location);
    }
    throw SneakyThrows.propagate(new FileNotFoundException(location.toAbsolutePath().toString()));
  }
}
