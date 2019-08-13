/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.AttachedFile;
import io.jooby.Context;
import io.jooby.MessageEncoder;
import io.jooby.ModelAndView;
import io.jooby.TemplateEngine;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CompositeMessageEncoder implements MessageEncoder {

  private List<MessageEncoder> encoder = new ArrayList<>(2);

  private List<TemplateEngine> templateEngine = new ArrayList<>(2);

  public CompositeMessageEncoder add(MessageEncoder encoder) {
    if (encoder instanceof TemplateEngine) {
      templateEngine.add((TemplateEngine) encoder);
    } else {
      this.encoder.add(encoder);
    }
    return this;
  }

  @Override public byte[] encode(@Nonnull Context ctx, @Nonnull Object value) throws Exception {
    if (value instanceof ModelAndView) {
      ModelAndView modelAndView = (ModelAndView) value;
      for (TemplateEngine engine : templateEngine) {
        if (engine.supports(modelAndView)) {
          return engine.encode(ctx, modelAndView);
        }
      }
      throw new IllegalArgumentException("No template engine for: " + modelAndView.getView());
    }
    /** InputStream: */
    if (value instanceof InputStream) {
      ctx.send((InputStream) value);
      return null;
    }
    /** FileChannel: */
    if (value instanceof FileChannel) {
      ctx.send((FileChannel) value);
      return null;
    }
    if (value instanceof File) {
      ctx.send(((File) value).toPath());
      return null;
    }
    if (value instanceof Path) {
      ctx.send((Path) value);
      return null;
    }
    /** Attached file: */
    if (value instanceof AttachedFile) {
      ctx.send((AttachedFile) value);
      return null;
    }
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
    Iterator<MessageEncoder> iterator = encoder.iterator();
    /** NOTE: looks like an infinite loop but there is a default renderer at the end of iterator. */
    byte[] result = null;
    while (result == null) {
      MessageEncoder next = iterator.next();
      result = next.encode(ctx, value);
    }
    return result;
  }
}
