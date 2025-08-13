/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.Body;
import io.jooby.Context;
import io.jooby.MediaType;
import io.jooby.value.Value;

public class ByteArrayBody implements Body {
  private static final byte[] EMPTY = new byte[0];

  protected final Context ctx;

  private final byte[] bytes;

  public ByteArrayBody(Context ctx, byte[] bytes) {
    this.ctx = ctx;
    this.bytes = bytes;
  }

  @Override
  public Value get(@NonNull String name) {
    return new MissingValue(ctx.getValueFactory(), name);
  }

  @Override
  public Value getOrDefault(@NonNull String name, @NonNull String defaultValue) {
    return Value.value(ctx.getValueFactory(), name, defaultValue);
  }

  @Override
  public long getSize() {
    return bytes.length;
  }

  @Override
  public @NonNull byte[] bytes() {
    return bytes;
  }

  @Override
  public @NonNull ReadableByteChannel channel() {
    return Channels.newChannel(stream());
  }

  @Override
  public boolean isInMemory() {
    return true;
  }

  @Override
  public @NonNull InputStream stream() {
    return new ByteArrayInputStream(bytes);
  }

  @NonNull @Override
  public String value() {
    return value(StandardCharsets.UTF_8);
  }

  @NonNull @Override
  public List<String> toList() {
    return Collections.singletonList(value());
  }

  @Override
  public String name() {
    return "body";
  }

  @NonNull @Override
  public <T> T to(@NonNull Type type) {
    return ctx.decode(type, ctx.getRequestType(MediaType.text));
  }

  @Nullable @Override
  public <T> T toNullable(@NonNull Type type) {
    return bytes.length == 0 ? null : ctx.decode(type, ctx.getRequestType(MediaType.text));
  }

  @Override
  public @NonNull Map<String, List<String>> toMultimap() {
    return Map.of();
  }

  public static Body empty(Context ctx) {
    return new ByteArrayBody(ctx, EMPTY);
  }
}
