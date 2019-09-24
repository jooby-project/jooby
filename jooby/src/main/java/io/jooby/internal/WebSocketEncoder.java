package io.jooby.internal;

import io.jooby.Context;
import io.jooby.MessageEncoder;
import io.jooby.TemplateEngine;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;

public class WebSocketEncoder implements MessageEncoder {
  private final List<MessageEncoder> encoderList;
  private final List<TemplateEngine> templatEngineList;

  public WebSocketEncoder(List<MessageEncoder> encoderList,
      List<TemplateEngine> templateEngineList) {
    this.encoderList = encoderList;
    this.templatEngineList = templateEngineList;
  }

  @Nonnull @Override public byte[] encode(@Nonnull Context ctx,
      @Nonnull Object value) throws Exception {
    /** Strings: */
    if (value instanceof CharSequence) {
      return value.toString().getBytes(StandardCharsets.UTF_8);
    }
    if (value instanceof Number) {
      return value.toString().getBytes(StandardCharsets.UTF_8);
    }
    /** RawByte: */
    if (value instanceof byte[]) {
      return (byte[]) value;
    }
    if (value instanceof ByteBuffer) {
      ctx.send((ByteBuffer) value);
      return null;
    }
    Iterator<MessageEncoder> iterator = encoderList.iterator();
    /** NOTE: looks like an infinite loop but there is a default renderer at the end of iterator. */
    byte[] result = null;
    while (result == null) {
      MessageEncoder next = iterator.next();
      result = next.encode(ctx, value);
    }
    return result;
  }
}
