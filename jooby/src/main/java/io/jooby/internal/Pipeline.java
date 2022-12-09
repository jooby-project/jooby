/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executor;

import io.jooby.Context;
import io.jooby.ExecutionMode;
import io.jooby.FileDownload;
import io.jooby.Reified;
import io.jooby.ResponseHandler;
import io.jooby.Route;
import io.jooby.Route.Handler;
import io.jooby.internal.handler.DefaultHandler;
import io.jooby.internal.handler.DetachHandler;
import io.jooby.internal.handler.DispatchHandler;
import io.jooby.internal.handler.PostDispatchInitializerHandler;
import io.jooby.internal.handler.SendAttachment;
import io.jooby.internal.handler.SendByteArray;
import io.jooby.internal.handler.SendByteBuffer;
import io.jooby.internal.handler.SendCharSequence;
import io.jooby.internal.handler.SendDirect;
import io.jooby.internal.handler.SendFileChannel;
import io.jooby.internal.handler.SendStream;
import io.jooby.internal.handler.WorkerHandler;

public class Pipeline {

  public static Handler build(
      Route route,
      ExecutionMode mode,
      Executor executor,
      ContextInitializer initializer,
      List<ResponseHandler> responseHandler) {
    if (route.isReactive()) {
      return next(
          mode, executor, new DetachHandler(decorate(initializer, route.getPipeline())), false);
    }
    Type returnType = route.getReturnType();
    if (returnType != null) {
      Class<?> type = Reified.rawType(returnType);
      /** Context: */
      if (Context.class.isAssignableFrom(type)) {
        if (executor == null && mode == ExecutionMode.EVENT_LOOP) {
          return next(
              mode, executor, decorate(initializer, new DetachHandler(route.getPipeline())), false);
        }
        return next(
            mode, executor, decorate(initializer, new SendDirect(route.getPipeline())), true);
      }
      /** InputStream: */
      if (InputStream.class.isAssignableFrom(type)) {
        return next(
            mode, executor, decorate(initializer, new SendStream(route.getPipeline())), true);
      }
      /** FileChannel: */
      if (FileChannel.class.isAssignableFrom(type)
          || Path.class.isAssignableFrom(type)
          || File.class.isAssignableFrom(type)) {
        return next(
            mode, executor, decorate(initializer, new SendFileChannel(route.getPipeline())), true);
      }
      /** FileDownload: */
      if (FileDownload.class.isAssignableFrom(type)) {
        return next(
            mode, executor, decorate(initializer, new SendAttachment(route.getPipeline())), true);
      }
      /** Strings: */
      if (CharSequence.class.isAssignableFrom(type)) {
        return next(
            mode, executor, decorate(initializer, new SendCharSequence(route.getPipeline())), true);
      }
      /** RawByte: */
      if (byte[].class == type) {
        return next(
            mode, executor, decorate(initializer, new SendByteArray(route.getPipeline())), true);
      }
      if (ByteBuffer.class.isAssignableFrom(type)) {
        return next(
            mode, executor, decorate(initializer, new SendByteBuffer(route.getPipeline())), true);
      }
    }
    if (responseHandler != null) {
      return responseHandler.stream()
          .filter(it -> it.matches(returnType))
          .findFirst()
          .map(
              factory ->
                  next(
                      mode,
                      executor,
                      decorate(initializer, factory.create(route.getPipeline())),
                      true))
          .orElseGet(
              () ->
                  next(
                      mode,
                      executor,
                      decorate(initializer, new DefaultHandler(route.getPipeline())),
                      true));
    }

    return next(
        mode, executor, decorate(initializer, new DefaultHandler(route.getPipeline())), true);
  }

  private static Handler decorate(ContextInitializer initializer, Handler handler) {
    Handler pipeline = handler;
    if (initializer == null) {
      return pipeline;
    }
    return new PostDispatchInitializerHandler(initializer, pipeline);
  }

  private static Handler next(
      ExecutionMode mode, Executor executor, Handler handler, boolean blocking) {
    if (executor == null) {
      if (mode == ExecutionMode.WORKER) {
        return new WorkerHandler(handler);
      }
      if (mode == ExecutionMode.DEFAULT && blocking) {
        return new WorkerHandler(handler);
      }
      return handler;
    }
    return new DispatchHandler(handler, executor);
  }
}
