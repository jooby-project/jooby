/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.Body;
import io.jooby.Context;
import io.jooby.MediaType;
import io.jooby.SneakyThrows;
import io.netty.handler.codec.http.multipart.HttpData;

public class NettyBody implements Body {
  private final Context ctx;
  private final HttpData data;
  private final long length;

  public NettyBody(Context ctx, HttpData data, long contentLength) {
    this.ctx = ctx;
    this.data = data;
    this.length = contentLength;
  }

  @Override
  public boolean isInMemory() {
    return data.isInMemory();
  }

  @Override
  public long getSize() {
    return length;
  }

  @Override
  public InputStream stream() {
    try {
      if (data.isInMemory()) {
        return new ByteArrayInputStream(data.get());
      }
      return new FileInputStream(data.getFile());
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  @Override
  public ReadableByteChannel channel() {
    return Channels.newChannel(stream());
  }

  @Override
  public byte[] bytes() {
    try {
      if (data.isInMemory()) {
        return data.get();
      }
      return Files.readAllBytes(data.getFile().toPath());
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
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
