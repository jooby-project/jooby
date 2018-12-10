package io.jooby.internal;

import io.jooby.Body;
import io.jooby.Value;

import javax.annotation.Nonnull;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BodyImpl implements Body {
  private long length;
  private InputStream in;

  public BodyImpl(InputStream stream, long contentLength) {
    this.in = stream;
    this.length = contentLength;
  }

  @Override public long contentLength() {
    return length;
  }

  @Override public InputStream stream() {
    return in;
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

  @Override public Map<String, List<String>> toMap() {
    return Collections.emptyMap();
  }
}
