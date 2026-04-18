/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

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

import org.jspecify.annotations.Nullable;

import io.jooby.Body;
import io.jooby.Context;
import io.jooby.MediaType;
import io.jooby.SneakyThrows;
import io.jooby.value.Value;

public class FileBody implements Body {
  private Context ctx;
  private Path file;

  public FileBody(Context ctx, Path file) {
    this.ctx = ctx;
    this.file = file;
  }

  @Override
  public long getSize() {
    try {
      return Files.size(file);
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  @Override
  public Value get(String name) {
    return new MissingValue(ctx.getValueFactory(), name);
  }

  @Override
  public Value getOrDefault(String name, String defaultValue) {
    return Value.value(ctx.getValueFactory(), name, defaultValue);
  }

  @Override
  public boolean isInMemory() {
    return false;
  }

  @Override
  public ReadableByteChannel channel() {
    try {
      return Files.newByteChannel(file);
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  @Override
  public InputStream stream() {
    try {
      return Files.newInputStream(file);
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  @Override
  public byte[] bytes() {
    try {
      return Files.readAllBytes(file);
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  @Override
  public String value() {
    return value(StandardCharsets.UTF_8);
  }

  @Override
  public List<String> toList() {
    return Collections.singletonList(value());
  }

  @Override
  public String name() {
    return "body";
  }

  @Override
  public <T> T to(Type type) {
    return ctx.decode(type, ctx.getRequestType(MediaType.text));
  }

  @Nullable @Override
  public <T> T toNullable(Type type) {
    return ctx.decode(type, ctx.getRequestType(MediaType.text));
  }

  @Override
  public Map<String, List<String>> toMultimap() {
    return Map.of();
  }
}
