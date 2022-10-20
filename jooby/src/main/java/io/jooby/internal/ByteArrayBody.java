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
import io.jooby.Body;
import io.jooby.Context;
import io.jooby.MediaType;
import io.jooby.ValueNode;

public class ByteArrayBody implements Body {
  private static final byte[] EMPTY = new byte[0];

  protected final Context ctx;

  private final byte[] bytes;

  public ByteArrayBody(Context ctx, byte[] bytes) {
    this.ctx = ctx;
    this.bytes = bytes;
  }

  @Override
  public long getSize() {
    return bytes.length;
  }

  @Override
  public byte[] bytes() {
    return bytes;
  }

  @Override
  public ReadableByteChannel channel() {
    return Channels.newChannel(stream());
  }

  @Override
  public boolean isInMemory() {
    return true;
  }

  @Override
  public InputStream stream() {
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

  @NonNull @Override
  public ValueNode get(@NonNull int index) {
    return index == 0 ? this : get(Integer.toString(index));
  }

  @NonNull @Override
  public ValueNode get(@NonNull String name) {
    return new MissingValue(name);
  }

  @Override
  public String name() {
    return "body";
  }

  @NonNull @Override
  public <T> T to(@NonNull Type type) {
    return ctx.decode(type, ctx.getRequestType(MediaType.text));
  }

  @Override
  public Map<String, List<String>> toMultimap() {
    return Collections.emptyMap();
  }

  public static Body empty(Context ctx) {
    return new ByteArrayBody(ctx, EMPTY);
  }
}
