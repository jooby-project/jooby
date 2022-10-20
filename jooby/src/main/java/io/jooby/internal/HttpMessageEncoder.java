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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.FileDownload;
import io.jooby.MediaType;
import io.jooby.MessageEncoder;
import io.jooby.ModelAndView;
import io.jooby.StatusCode;
import io.jooby.TemplateEngine;

public class HttpMessageEncoder implements MessageEncoder {

  private Map<MediaType, MessageEncoder> encoders;

  private List<TemplateEngine> templateEngineList = new ArrayList<>(2);

  public HttpMessageEncoder add(MediaType type, MessageEncoder encoder) {
    if (encoder instanceof TemplateEngine) {
      // media type is ignored for template engines. They  have a custom object type
      templateEngineList.add((TemplateEngine) encoder);
    } else {
      if (encoders == null) {
        encoders = new LinkedHashMap<>();
      }
      encoders.put(type, encoder);
    }
    return this;
  }

  @Override
  public byte[] encode(@NonNull Context ctx, @NonNull Object value) throws Exception {
    if (value instanceof ModelAndView) {
      ModelAndView modelAndView = (ModelAndView) value;
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
