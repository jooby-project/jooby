/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.handler.Asset;
import io.jooby.handler.AssetSource;

public class ClassPathAssetSource implements AssetSource {

  private final ClassLoader loader;

  private final String source;

  private final boolean isDir;

  private final String prefix;

  public ClassPathAssetSource(ClassLoader loader, String source) {
    if (source == null || source.trim().isEmpty() || source.trim().equals("/")) {
      throw new IllegalArgumentException(
          "For security reasons root classpath access is not allowed: " + source);
    }
    this.loader = loader;
    this.source = source.startsWith("/") ? source.substring(1) : source;
    this.prefix = sourcePrefix(this.source);
    isDir = isDirectory(loader, this.source);
  }

  @Nullable @Override
  public Asset resolve(@NonNull String path) {
    String fullpath;
    if (isDir) {
      fullpath = safePath(prefix + path);
      if (!fullpath.startsWith(prefix)) {
        return null;
      }
    } else {
      fullpath = source;
    }

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
      // See https://github.com/jooby-project/jooby/issues/2660
      connection.setDefaultUseCaches(false);
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

  private static String safePath(String path) {
    if (path.indexOf("./") > 0) {
      return normalize(path.split("/"));
    }
    return path;
  }

  private static String normalize(String[] segments) {
    Path path = Paths.get(segments[0]);
    for (int i = 1; i < segments.length; i++) {
      path = path.resolve(segments[i]);
    }
    StringBuilder buffer = new StringBuilder();
    for (Path segment : path.normalize()) {
      buffer.append("/").append(segment);
    }
    return buffer.substring(1);
  }
}
