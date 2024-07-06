/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import static io.jooby.internal.handler.DefaultHandler.DEFAULT;
import static io.jooby.internal.handler.DetachHandler.DETACH;
import static io.jooby.internal.handler.WorkerHandler.WORKER;

import java.util.concurrent.Executor;

import io.jooby.ExecutionMode;
import io.jooby.Route;
import io.jooby.Route.Handler;
import io.jooby.internal.handler.DispatchHandler;
import io.jooby.internal.handler.PostDispatchInitializerHandler;

public class Pipeline {

  public static Handler build(
      boolean requiresDetach,
      Route route,
      ExecutionMode mode,
      Executor executor,
      ContextInitializer initializer) {
    // Set default wrapper and blocking mode
    if (!route.isNonBlockingSet()) {
      route.setNonBlocking(isDefaultNonblocking(executor, mode));
    }
    Route.Filter wrapper = DEFAULT;
    if (requiresDetach && route.isNonBlocking()) {
      wrapper = DETACH.then(wrapper);
    }

    // Non-Blocking? Split pipeline Head+Handler let reactive call After pipeline
    Handler pipeline;
    if (route.isNonBlocking()) {
      Route.Filter pre = route.getFilter();
      pipeline = pre == null ? route.getHandler() : pre.then(route.getHandler());
    } else {
      pipeline = route.getPipeline();
    }
    return dispatchHandler(
        mode, executor, decorate(initializer, wrapper.then(pipeline)), route.isNonBlocking());
  }

  private static boolean isDefaultNonblocking(Executor executor, ExecutionMode mode) {
    return executor == null && mode == ExecutionMode.EVENT_LOOP;
  }

  private static Handler decorate(ContextInitializer initializer, Handler handler) {
    return initializer == null
        ? handler
        : new PostDispatchInitializerHandler(initializer).then(handler);
  }

  private static Handler dispatchHandler(
      ExecutionMode mode, Executor executor, Handler handler, boolean nonblocking) {
    if (executor == null) {
      if (mode == ExecutionMode.WORKER) {
        return WORKER.then(handler);
      }
      if (mode == ExecutionMode.DEFAULT && !nonblocking) {
        return WORKER.then(handler);
      }
      return handler;
    }
    return new DispatchHandler(executor).apply(handler);
  }
}
