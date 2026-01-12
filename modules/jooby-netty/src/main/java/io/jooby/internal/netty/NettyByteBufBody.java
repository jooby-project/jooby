/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

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
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufUtil;

public class NettyByteBufBody implements Body {
  private final Context ctx;
  private final ByteBuf data;
  private final long length;

  public NettyByteBufBody(Context ctx, ByteBuf data) {
    this.ctx = ctx;
    this.data = data;
    this.length = data.readableBytes();
  }

  @Override
  public boolean isInMemory() {
    return true;
  }

  @Override
  public long getSize() {
    return length;
  }

  @Override
  public InputStream stream() {
    return new ByteBufInputStream(data);
  }

  @Override
  public Value get(@NonNull String name) {
    return Value.missing(ctx.getValueFactory(), name);
  }

  @Override
  public Value getOrDefault(@NonNull String name, @NonNull String defaultValue) {
    return Value.value(ctx.getValueFactory(), name, defaultValue);
  }

  @Override
  public ReadableByteChannel channel() {
    return Channels.newChannel(stream());
  }

  @Override
  public byte[] bytes() {
    return ByteBufUtil.getBytes(data);
  }

  @NonNull @Override
  public String value() {
    return value(StandardCharsets.UTF_8);
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
    return ctx.decode(type, ctx.getRequestType(MediaType.text));
  }

  @Override
  public Map<String, List<String>> toMultimap() {
    return Collections.emptyMap();
  }
}
