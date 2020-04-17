/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.Asset;
import io.jooby.AssetSource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class ClassPathAssetSource implements AssetSource {

  private final ClassLoader loader;

  private final String source;

  private final boolean isDir;

  private final String prefix;

  public ClassPathAssetSource(ClassLoader loader, String source) {
    if (source == null || source.trim().length() == 0 || source.trim().equals("/")) {
      throw new IllegalArgumentException(
          "For security reasons root classpath access is not allowed: " + source);
    }
    this.loader = loader;
    this.source = source.startsWith("/") ? source.substring(1) : source;
    this.prefix = sourcePrefix(this.source);
    isDir = isDirectory(loader, this.source);
  }

  @Nullable @Override public Asset resolve(@Nonnull String path) {
    String fullpath = isDir ? prefix + path : source;
    URL resource = loader.getResource(fullpath);
    if (resource == null) {
      return null;
    }
    Asset asset = Asset.create(fullpath, resource);
    if (asset.isDirectory()) {
      // try index.html
      fullpath += "/index.html";
      resource = loader.getResource(fullpath);
      if (resource != null) {
        asset = Asset.create(fullpath, resource);
      } else {
        asset = null;
      }
    }
    return asset;
  }

  private String sourcePrefix(String path) {
    if (path.length() > 0 && !path.endsWith("/")) {
      return path + "/";
    }
    return path;
  }

  private boolean isDirectory(ClassLoader loader, String base) {
    try {
      URL url = loader.getResource(base);
      if (url == null) {
        return true;
      }
      URLConnection connection = url.openConnection();
      if (connection instanceof JarURLConnection) {
        JarURLConnection jarConnection = (JarURLConnection) connection;
        try (JarFile jar = jarConnection.getJarFile()) {
          ZipEntry entry = jar.getEntry(base);
          return entry.isDirectory();
        }
      }
      return Files.isDirectory(Paths.get(url.toURI()));
    } catch (Exception x) {
      return true;
    }
  }
}
