package io.jooby.internal.netty;

import io.jooby.Body;
import io.jooby.Throwing;
import io.jooby.Value;
import io.netty.handler.codec.http.multipart.HttpData;
import io.netty.handler.codec.http.multipart.MixedAttribute;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class NettyBody implements Body {
  private final HttpData data;
  private long length;

  public NettyBody(HttpData data, long contentLength) {
    this.data = data;
    this.length = contentLength;
  }

  @Override public long contentLength() {
    return length;
  }

  @Override public InputStream stream() {
    try {
      if (data.isInMemory()) {
        return new ByteArrayInputStream(data.get());
      }
      return new FileInputStream(data.getFile());
    } catch (IOException x) {
      throw Throwing.sneakyThrow(x);
    }
  }

  @Nonnull @Override public String value() {
    return value(StandardCharsets.UTF_8);
  }

  @Nonnull @Override public Value get(@Nonnull int index) {
    return index == 0 ? this : get(Integer.toString(index));
  }

  @Nonnull @Override public Value get(@Nonnull String name) {
    return new Missing(name);
  }

  @Override public String name() {
    return "body";
  }

  @Override public Map<String, List<String>> toMultimap() {
    return Collections.emptyMap();
  }
}
