/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jstachio;

import java.io.IOException;

import io.jooby.Context;
import io.jooby.MediaType;
import io.jstach.jstachio.JStachio;
import io.jstach.jstachio.Template;

/**
 * Shared logic between encoder and result handler
 *
 * @author agentgt
 * @param <T> results of render call
 */
abstract class JStachioRenderer<T> {

  private final JStachio jstachio;
  private final JStachioBuffer buffer;

  public JStachioRenderer(JStachio jstachio, JStachioBuffer buffer) {
    super();
    this.jstachio = jstachio;
    this.buffer = buffer;
  }

  public T render(Context ctx, Object model) throws Exception {
    var stream = buffer.acquire();
    try {
      @SuppressWarnings("rawtypes")
      Template template = jstachio.findTemplate(model);
      /*
       * TODO we probably should resolve the correct media type here and more importantly charset
       * Or at least validate that it is text/html UTF-8.
       * However this would slow things down.
       *
       * The Rocker module apparently just assumes "text/html; charset=utf-8"
       * So for now we will as well.
       */
      ctx.setResponseType(MediaType.html);
      return render(ctx, template, model, stream);
    } finally {
      buffer.release(stream);
    }
  }

  abstract T render(
      Context ctx,
      @SuppressWarnings("rawtypes") Template template,
      Object model,
      ByteBufferedOutputStream stream)
      throws IOException;

  protected boolean supportsType(Class<?> modelClass) {
    return jstachio.supportsType(modelClass);
  }
}
