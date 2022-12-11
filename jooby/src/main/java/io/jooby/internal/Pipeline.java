/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import static io.jooby.ReactiveSupport.concurrent;
import static io.jooby.internal.handler.DefaultHandler.DEFAULT;
import static io.jooby.internal.handler.DetachHandler.DETACH;
import static io.jooby.internal.handler.WorkerHandler.WORKER;

import java.lang.reflect.Type;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;

import io.jooby.Context;
import io.jooby.ExecutionMode;
import io.jooby.Reified;
import io.jooby.ResultHandler;
import io.jooby.Route;
import io.jooby.Route.Handler;
import io.jooby.internal.handler.DispatchHandler;
import io.jooby.internal.handler.PostDispatchInitializerHandler;
import io.jooby.internal.handler.SendDirect;

public class Pipeline {

  public static Handler build(
      Route route,
      ExecutionMode mode,
      Executor executor,
      ContextInitializer initializer,
      Set<ResultHandler> responseHandler) {
    // Set default wrapper and blocking mode
    Route.Filter wrapper = route.isReactive() ? DETACH : DEFAULT;
    boolean blocking = !route.isReactive();

    /** Return type is set by annotation processor, or manually per lambda route: */
    Type returnType = route.getReturnType();
    if (returnType != null) {
      Class<?> type = Reified.rawType(returnType);
      /** Context: */
      if (Context.class.isAssignableFrom(type)) {
        if (executor == null && mode == ExecutionMode.EVENT_LOOP) {
          blocking = false;
          wrapper = DETACH;
        } else {
          blocking = true;
          wrapper = SendDirect.DIRECT;
        }
      } else if (CompletionStage.class.isAssignableFrom(type)
          || Flow.Publisher.class.isAssignableFrom(type)) {
        /** Completable future: */
        blocking = false;
        wrapper = DETACH.then(concurrent());
      } else {
        /** Custom responses: */
        for (ResultHandler factory : responseHandler) {
          if (factory.matches(returnType)) {
            Route.Filter custom = factory.create();
            if (factory.isReactive()) {
              blocking = false;
              wrapper = DETACH.then(custom);
            } else {
              blocking = true;
              wrapper = custom;
            }
            break;
          }
        }
      }
    }
    return dispatchHandler(
        mode, executor, decorate(initializer, wrapper.then(route.getPipeline())), blocking);
  }

  private static Handler decorate(ContextInitializer initializer, Handler handler) {
    return initializer == null
        ? handler
        : new PostDispatchInitializerHandler(initializer).then(handler);
  }

  private static Handler dispatchHandler(
      ExecutionMode mode, Executor executor, Handler handler, boolean blocking) {
    if (executor == null) {
      if (mode == ExecutionMode.WORKER) {
        return WORKER.then(handler);
      }
      if (mode == ExecutionMode.DEFAULT && blocking) {
        return WORKER.then(handler);
      }
      return handler;
    }
    return new DispatchHandler(executor).apply(handler);
  }
}
