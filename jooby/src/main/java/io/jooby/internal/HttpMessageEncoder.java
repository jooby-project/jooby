/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.FileDownload;
import io.jooby.MediaType;
import io.jooby.MessageEncoder;
import io.jooby.ModelAndView;
import io.jooby.StatusCode;
import io.jooby.TemplateEngine;
import io.jooby.buffer.DataBuffer;

public class HttpMessageEncoder implements MessageEncoder {

  private Map<MediaType, MessageEncoder> encoders;

  private final LinkedList<TemplateEngine> templateEngineList = new LinkedList<>();

  public HttpMessageEncoder add(MediaType type, MessageEncoder encoder) {
    if (encoder instanceof TemplateEngine engine) {
      // media type is ignored for template engines. They  have a custom object type
      templateEngineList.add(engine);
    } else {
      if (encoders == null) {
        encoders = new LinkedHashMap<>();
      }
      encoders.put(type, encoder);
    }
    return this;
  }

  @Override
  public DataBuffer encode(@NonNull Context ctx, @NonNull Object value) throws Exception {
    if (value instanceof ModelAndView modelAndView) {
      for (TemplateEngine engine : templateEngineList) {
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
    /** StatusCode: */
    if (value instanceof StatusCode) {
      ctx.send((StatusCode) value);
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
    /** FileDownload: */
    if (value instanceof FileDownload) {
      ctx.send((FileDownload) value);
      return null;
    }
    var bufferFactory = ctx.getBufferFactory();
    /** Strings: */
    if (value instanceof CharSequence) {
      return bufferFactory.wrap(value.toString().getBytes(StandardCharsets.UTF_8));
    }
    if (value instanceof Number) {
      return bufferFactory.wrap(value.toString().getBytes(StandardCharsets.UTF_8));
    }
    /** RawByte: */
    if (value instanceof byte[]) {
      return bufferFactory.wrap((byte[]) value);
    }
    if (value instanceof ByteBuffer) {
      ctx.send((ByteBuffer) value);
      return null;
    }
    if (encoders != null) {
      // Content negotiation, find best:
      List<MediaType> produces = ctx.getRoute().getProduces();
      if (produces.isEmpty()) {
        produces = new ArrayList<>(encoders.keySet());
      }
      MediaType type = ctx.accept(produces);
      MessageEncoder encoder = encoders.getOrDefault(type, MessageEncoder.TO_STRING);
      return encoder.encode(ctx, value);
    } else {
      return MessageEncoder.TO_STRING.encode(ctx, value);
    }
  }
}
