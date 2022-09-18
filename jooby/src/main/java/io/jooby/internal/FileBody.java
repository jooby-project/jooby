/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.Body;
import io.jooby.Context;
import io.jooby.MediaType;
import io.jooby.SneakyThrows;
import io.jooby.ValueNode;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class FileBody implements Body {
  private Context ctx;
  private Path file;

  public FileBody(Context ctx, Path file) {
    this.ctx = ctx;
    this.file = file;
  }

  @Override public long getSize() {
    try {
    return Files.size(file);
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  @Override public boolean isInMemory() {
    return false;
  }

  @Override public ReadableByteChannel channel() {
    try {
      return Files.newByteChannel(file);
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  @Override public InputStream stream() {
    try {
      return Files.newInputStream(file);
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  @Override public byte[] bytes() {
    try {
      return Files.readAllBytes(file);
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  @NonNull @Override public String value() {
    return value(StandardCharsets.UTF_8);
  }

  @NonNull @Override public List<String> toList() {
    return Collections.singletonList(value());
  }

  @NonNull @Override public ValueNode get(@NonNull int index) {
    return index == 0 ? this : get(Integer.toString(index));
  }

  @NonNull @Override public ValueNode get(@NonNull String name) {
    return new MissingValue(name);
  }

  @Override public String name() {
    return "body";
  }

  @NonNull @Override public <T> T to(@NonNull Type type) {
    return ctx.decode(type, ctx.getRequestType(MediaType.text));
  }

  @Override public Map<String, List<String>> toMultimap() {
    return Collections.emptyMap();
  }
}
