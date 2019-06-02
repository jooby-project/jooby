/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.Body;
import io.jooby.Sneaky;
import io.jooby.Value;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class FileBody implements Body {
  private Path file;

  public FileBody(Path file) {
    this.file = file;
  }

  @Override public long getSize() {
    try {
    return Files.size(file);
    } catch (IOException x) {
      throw Sneaky.propagate(x);
    }
  }

  @Override public boolean isInMemory() {
    return false;
  }

  @Override public ReadableByteChannel channel() {
    try {
      return Files.newByteChannel(file);
    } catch (IOException x) {
      throw Sneaky.propagate(x);
    }
  }

  @Override public InputStream stream() {
    try {
      return Files.newInputStream(file);
    } catch (IOException x) {
      throw Sneaky.propagate(x);
    }
  }

  @Override public byte[] bytes() {
    try {
      return Files.readAllBytes(file);
    } catch (IOException x) {
      throw Sneaky.propagate(x);
    }
  }

  @Nonnull @Override public String value() {
    return value(StandardCharsets.UTF_8);
  }

  @Nonnull @Override public Value get(@Nonnull int index) {
    return index == 0 ? this : get(Integer.toString(index));
  }

  @Nonnull @Override public Value get(@Nonnull String name) {
    return new MissingValue(name);
  }

  @Override public String name() {
    return "body";
  }

  @Override public Map<String, List<String>> toMultimap() {
    return Collections.emptyMap();
  }
}
