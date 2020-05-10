package org.jooby.internal;

import com.google.common.base.Strings;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public interface AssetSource {
  URL getResource(String name);

  static AssetSource fromClassPath(ClassLoader loader, String source) {
    if (Strings.isNullOrEmpty(source) || "/".equals(source.trim())) {
      throw new IllegalArgumentException(
          "For security reasons root classpath access is not allowed: " + source);
    }
    return path -> {
      URL resource = loader.getResource(path);
      if (resource == null) {
        return null;
      }
      String realPath = resource.getPath();
      if (realPath.startsWith(source)) {
        return resource;
      }
      return null;
    };
  }

  static AssetSource fromFileSystem(Path basedir) {
    return name -> {
      Path path = basedir.resolve(name).normalize();
      if (Files.exists(path) && path.startsWith(basedir)) {
        try {
          return path.toUri().toURL();
        } catch (MalformedURLException x) {
          // shh
        }
      }
      return null;
    };
  }
}
