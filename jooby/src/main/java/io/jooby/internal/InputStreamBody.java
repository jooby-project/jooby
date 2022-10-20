/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
import io.jooby.ServerOptions;
import io.jooby.SneakyThrows;
import io.jooby.ValueNode;

public class InputStreamBody implements Body {
  private Context ctx;
  private long length;
  private InputStream in;

  public InputStreamBody(Context ctx, InputStream stream, long contentLength) {
    this.ctx = ctx;
    this.in = stream;
    this.length = contentLength;
  }

  public byte[] bytes() {
    try (InputStream stream = in) {
      int bufferSize = ServerOptions._16KB;
      ByteArrayOutputStream out = new ByteArrayOutputStream(bufferSize);
      int len;
      byte[] buffer = new byte[bufferSize];
      while ((len = stream.read(buffer, 0, buffer.length)) != -1) {
        out.write(buffer, 0, len);
      }
      return out.toByteArray();
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  @Override
  public boolean isInMemory() {
    return false;
  }

  @Override
  public ReadableByteChannel channel() {
    return Channels.newChannel(in);
  }

  @Override
  public long getSize() {
    return length;
  }

  @Override
  public InputStream stream() {
    return in;
  }

  @NonNull @Override
  public String value() {
    return value(StandardCharsets.UTF_8);
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

  @NonNull @Override
  public List<String> toList() {
    return Collections.singletonList(value());
  }

  @Override
  public Map<String, List<String>> toMultimap() {
    return Collections.emptyMap();
  }
}
