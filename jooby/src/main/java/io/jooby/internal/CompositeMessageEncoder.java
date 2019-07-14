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
import io.jooby.internal.handler.SendAttachment;
import io.jooby.internal.handler.SendByteArray;
import io.jooby.internal.handler.SendByteBuf;
import io.jooby.internal.handler.SendByteBuffer;
import io.jooby.internal.handler.SendCharSequence;
import io.jooby.internal.handler.SendDirect;
import io.jooby.internal.handler.SendFileChannel;
import io.jooby.internal.handler.SendStream;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedList;

public class CompositeMessageEncoder implements MessageEncoder {

  private LinkedList<MessageEncoder> list = new LinkedList<>();

  private MessageEncoder templateEngine;

  public CompositeMessageEncoder() {
    list.add(MessageEncoder.TO_STRING);
  }

  public CompositeMessageEncoder add(MessageEncoder encoder) {
    if (encoder instanceof TemplateEngine) {
      templateEngine = computeRenderer(templateEngine, encoder);
    } else {
      list.addFirst(encoder);
    }
    return this;
  }

  private MessageEncoder computeRenderer(MessageEncoder it, MessageEncoder next) {
    if (it == null) {
      return next;
    } else if (it instanceof CompositeMessageEncoder) {
      ((CompositeMessageEncoder) it).list.addFirst(next);
      return it;
    } else {
      CompositeMessageEncoder composite = new CompositeMessageEncoder();
      composite.list.addFirst(it);
      composite.list.addFirst(next);
      return composite;
    }
  }

  @Override public byte[] encode(@Nonnull Context ctx, @Nonnull Object value) throws Exception {
    if (value instanceof ModelAndView) {
      return templateEngine.encode(ctx, value);
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
    if (value instanceof ByteBuf) {
      ctx.send((ByteBuf) value);
      return null;
    }
    /** Fallback to more complex encoder: */
    Iterator<MessageEncoder> iterator = list.iterator();
    /** NOTE: looks like an infinite loop but there is a default renderer at the end of iterator. */
    byte[] result = null;
    while (result == null) {
      MessageEncoder next = iterator.next();
      result = next.encode(ctx, value);
    }
    return result;
  }
}
