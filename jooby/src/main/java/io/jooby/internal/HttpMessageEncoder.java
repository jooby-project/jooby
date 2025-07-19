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
import io.jooby.buffer.BufferedOutput;

public class HttpMessageEncoder implements MessageEncoder {

  private Map<MediaType, MessageEncoder> encoders;

  private final LinkedList<TemplateEngine> templateEngineList = new LinkedList<>();

  public HttpMessageEncoder add(MediaType type, MessageEncoder encoder) {
    if (encoder instanceof TemplateEngine engine) {
      // Media type is ignored for template engines. They have a custom object type
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
  public BufferedOutput encode(@NonNull Context ctx, @NonNull Object value) throws Exception {
    if (value instanceof ModelAndView<?> modelAndView) {
      for (var engine : templateEngineList) {
        if (engine.supports(modelAndView)) {
          return engine.encode(ctx, modelAndView);
        }
      }
      throw new IllegalArgumentException("No template engine for: " + modelAndView.getView());
    }
    /* InputStream: */
    if (value instanceof InputStream in) {
      ctx.send(in);
      return null;
    }
    /* StatusCode: */
    if (value instanceof StatusCode statusCode) {
      ctx.send(statusCode);
      return null;
    }
    /* FileChannel: */
    if (value instanceof FileChannel channel) {
      ctx.send(channel);
      return null;
    }
    if (value instanceof File file) {
      ctx.send(file.toPath());
      return null;
    }
    if (value instanceof Path path) {
      ctx.send(path);
      return null;
    }
    /* FileDownload: */
    if (value instanceof FileDownload download) {
      ctx.send(download);
      return null;
    }
    var outputFactory = ctx.getOutputFactory();
    /* Strings: */
    if (value instanceof CharSequence charSequence) {
      return outputFactory.wrap(charSequence.toString());
    }
    if (value instanceof Number) {
      return outputFactory.wrap(value.toString());
    }
    /* RawByte: */
    if (value instanceof byte[] bytes) {
      return outputFactory.wrap(bytes);
    }
    if (value instanceof ByteBuffer buffer) {
      return outputFactory.wrap(buffer);
    }
    if (encoders != null) {
      // Content negotiation, find best:
      var produces = ctx.getRoute().getProduces();
      if (produces.isEmpty()) {
        produces = encoders.keySet().stream().toList();
      }
      var type = ctx.accept(produces);
      var encoder = encoders.getOrDefault(type, MessageEncoder.TO_STRING);
      return encoder.encode(ctx, value);
    } else {
      return MessageEncoder.TO_STRING.encode(ctx, value);
    }
  }
}
