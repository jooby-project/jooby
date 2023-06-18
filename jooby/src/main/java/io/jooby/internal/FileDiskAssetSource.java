/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import java.nio.file.Path;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.handler.Asset;
import io.jooby.handler.AssetSource;

public class FileDiskAssetSource implements AssetSource {
  private Path filepath;

  public FileDiskAssetSource(@NonNull Path filepath) {
    this.filepath = filepath;
  }

  @Nullable @Override
  public Asset resolve(@NonNull String path) {
    return Asset.create(filepath);
  }

  @Override
  public String toString() {
    return filepath.toString();
  }
}
