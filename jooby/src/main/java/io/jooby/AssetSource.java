package io.jooby;

import javax.annotation.Nonnull;
import java.io.FileNotFoundException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public interface AssetSource {

  Asset resolve(String path);

  static AssetSource create(@Nonnull ClassLoader loader, @Nonnull String location) {
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

  static AssetSource create(@Nonnull Path location) {
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
    if (Files.isRegularFile(location )) {
      Asset singleFile = Asset.create(absoluteLocation);
      return p -> singleFile;
    }
    throw Throwing.sneakyThrow(new FileNotFoundException(location.toAbsolutePath().toString()));
  }
}
