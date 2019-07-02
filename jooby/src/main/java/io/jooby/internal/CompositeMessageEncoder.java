/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.Context;
import io.jooby.MessageEncoder;
import io.jooby.ModelAndView;
import io.jooby.TemplateEngine;

import javax.annotation.Nonnull;
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
    } else {
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
}
