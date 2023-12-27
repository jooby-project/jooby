/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jstachio;

import java.io.IOException;
import java.util.function.BiFunction;

import io.jooby.Context;
import io.jooby.MediaType;
import io.jstach.jstachio.JStachio;
import io.jstach.jstachio.context.ContextJStachio;
import io.jstach.jstachio.context.ContextNode;
import io.jstach.jstachio.output.ByteBufferEncodedOutput;

/**
 * Shared logic between encoder and result handler
 *
 * @author agentgt
 * @param <T> results of render call
 */
abstract class JStachioRenderer<T> {

  private final ContextJStachio jstachio;
  private final JStachioBuffer buffer;
  private final BiFunction<Context, String, String> contextFunction;

  public JStachioRenderer(
      JStachio jstachio,
      JStachioBuffer buffer,
      BiFunction<Context, String, String> contextFunction) {
    super();
    this.jstachio = ContextJStachio.of(jstachio);
    this.buffer = buffer;
    this.contextFunction = contextFunction;
  }

  public T render(Context ctx, Object model) throws Exception {
    var stream = buffer.acquire();
    try {
      /*
       * TODO we probably should resolve the correct media type here and more importantly charset
       * Or at least validate that it is text/html UTF-8.
       * However this would slow things down.
       *
       * The Rocker module apparently just assumes "text/html; charset=utf-8"
       * So for now we will as well.
       */
      ctx.setResponseType(MediaType.html);
      ContextNode contextNode = ContextNode.of(s -> contextFunction.apply(ctx, s));
      jstachio.write(model, contextNode, stream);
      return extractOutput(ctx, stream);
    } finally {
      buffer.release(stream);
    }
  }

  abstract T extractOutput(Context ctx, ByteBufferEncodedOutput stream) throws IOException;

  protected boolean supportsType(Class<?> modelClass) {
    return jstachio.supportsType(modelClass);
  }
}
