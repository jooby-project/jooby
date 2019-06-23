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
import java.nio.file.Path;

public class FileDiskAssetSource implements AssetSource {
  private Path filepath;

  public FileDiskAssetSource(@Nonnull Path filepath) {
    this.filepath = filepath;
  }

  @Nullable @Override public Asset resolve(@Nonnull String path) {
    return Asset.create(filepath);
  }

  @Override public String toString() {
    return filepath.toString();
  }
}
