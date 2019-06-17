package io.jooby.internal;

import io.jooby.Asset;
import io.jooby.AssetSource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Path;

public class FolderDiskAssetSource implements AssetSource {
  private Path location;

  public FolderDiskAssetSource(@Nonnull Path location) {
    this.location = location.normalize().toAbsolutePath();
  }

  @Nullable @Override public Asset resolve(@Nonnull String path) {
    Path resource = location.resolve(path).normalize().toAbsolutePath();
    if (resource.startsWith(location)) {
      if (Files.isRegularFile(resource)) {
        return Asset.create(resource);
      }
      if (Files.isDirectory(resource)) {
        Path index = resource.resolve("index.html");
        if (Files.isRegularFile(index)) {
          return Asset.create(index);
        }
      }
    }
    return null;
  }

  @Override public String toString() {
    return location.toString();
  }
}
