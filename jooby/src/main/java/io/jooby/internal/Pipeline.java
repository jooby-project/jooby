/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.Context;
import io.jooby.ExecutionMode;
import io.jooby.FileDownload;
import io.jooby.Reified;
import io.jooby.ResponseHandler;
import io.jooby.Route;
import io.jooby.Route.Handler;
import io.jooby.internal.handler.CompletionStageHandler;
import io.jooby.internal.handler.DefaultHandler;
import io.jooby.internal.handler.DetachHandler;
import io.jooby.internal.handler.DispatchHandler;
import io.jooby.internal.handler.PostDispatchInitializerHandler;
import io.jooby.internal.handler.KotlinJobHandler;
import io.jooby.internal.handler.SendAttachment;
import io.jooby.internal.handler.SendByteArray;
import io.jooby.internal.handler.SendByteBuffer;
import io.jooby.internal.handler.SendCharSequence;
import io.jooby.internal.handler.SendDirect;
import io.jooby.internal.handler.SendFileChannel;
import io.jooby.internal.handler.SendStream;
import io.jooby.internal.handler.WorkerHandler;
import io.jooby.internal.handler.reactive.ObservableHandler;
import io.jooby.internal.handler.reactive.ReactivePublisherHandler;
import io.jooby.internal.handler.reactive.ReactorFluxHandler;
import io.jooby.internal.handler.reactive.ReactorMonoHandler;
import io.jooby.internal.handler.reactive.RxFlowableHandler;
import io.jooby.internal.handler.reactive.RxMaybeHandler;
import io.jooby.internal.handler.reactive.RxSingleHandler;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

public class Pipeline {

  public static Handler compute(ClassLoader loader, Route route, ExecutionMode mode,
      Executor executor, ContextInitializer initializer, List<ResponseHandler> responseHandler) {
    Type returnType = route.getReturnType();
    Class<?> type = Reified.rawType(returnType);
    if (CompletionStage.class.isAssignableFrom(type)) {
      return completableFuture(mode, route, executor, initializer);
    }
    /** Rx 2: */
    // Single:
    Optional<Class> single = loadClass(loader, "io.reactivex.Single");
    if (single.isPresent()) {
      if (single.get().isAssignableFrom(type)) {
        return single(mode, route, executor, initializer);
      }
    }
    // Maybe:
    Optional<Class> maybe = loadClass(loader, "io.reactivex.Maybe");
    if (maybe.isPresent()) {
      if (maybe.get().isAssignableFrom(type)) {
        return rxMaybe(mode, route, executor, initializer);
      }
    }
    // Flowable:
    Optional<Class> flowable = loadClass(loader, "io.reactivex.Flowable");
    if (flowable.isPresent()) {
      if (flowable.get().isAssignableFrom(type)) {
        return rxFlowable(mode, route, executor, initializer);
      }
    }
    // Observable:
    Optional<Class> observable = loadClass(loader, "io.reactivex.Observable");
    if (observable.isPresent()) {
      if (observable.get().isAssignableFrom(type)) {
        return rxObservable(mode, route, executor, initializer);
      }
    }
    // Disposable
    Optional<Class> disposable = loadClass(loader, "io.reactivex.disposables.Disposable");
    if (disposable.isPresent()) {
      if (disposable.get().isAssignableFrom(type)) {
        return rxDisposable(mode, route, executor, initializer);
      }
    }
    /** Reactor: */
    // Flux:
    Optional<Class> flux = loadClass(loader, "reactor.core.publisher.Flux");
    if (flux.isPresent()) {
      if (flux.get().isAssignableFrom(type)) {
        return reactorFlux(mode, route, executor, initializer);
      }
    }
    // Mono:
    Optional<Class> mono = loadClass(loader, "reactor.core.publisher.Mono");
    if (mono.isPresent()) {
      if (mono.get().isAssignableFrom(type)) {
        return reactorMono(mode, route, executor, initializer);
      }
    }
    /** Kotlin: */
    Optional<Class> deferred = loadClass(loader, "kotlinx.coroutines.Deferred");
    if (deferred.isPresent()) {
      if (deferred.get().isAssignableFrom(type)) {
        return kotlinJob(mode, route, executor, initializer);
      }
    }
    Optional<Class> job = loadClass(loader, "kotlinx.coroutines.Job");
    if (job.isPresent()) {
      if (job.get().isAssignableFrom(type)) {
        return kotlinJob(mode, route, executor, initializer);
      }
    }
    Optional<Class> continuation = loadClass(loader, "kotlin.coroutines.Continuation");
    if (continuation.isPresent()) {
      if (continuation.get().isAssignableFrom(type)) {
        return kotlinContinuation(mode, route, executor, initializer);
      }
    }

    /** ReactiveStream: */
    Optional<Class> publisher = loadClass(loader, "org.reactivestreams.Publisher");
    if (publisher.isPresent()) {
      if (publisher.get().isAssignableFrom(type)) {
        return reactivePublisher(mode, route, executor, initializer);
      }
    }
    /** Context: */
    if (Context.class.isAssignableFrom(type)) {
      if (executor == null && mode == ExecutionMode.EVENT_LOOP) {
        return next(mode, executor, new DetachHandler(route.getPipeline()), false);
      }
      return next(mode, executor, decorate(route, initializer, new SendDirect(route.getPipeline())), true);
    }
    /** InputStream: */
    if (InputStream.class.isAssignableFrom(type)) {
      return next(mode, executor, decorate(route, initializer, new SendStream(route.getPipeline())), true);
    }
    /** FileChannel: */
    if (FileChannel.class.isAssignableFrom(type) || Path.class.isAssignableFrom(type) || File.class
        .isAssignableFrom(type)) {
      return next(mode, executor, decorate(route, initializer, new SendFileChannel(route.getPipeline())), true);
    }
    /** FileDownload: */
    if (FileDownload.class.isAssignableFrom(type)) {
      return next(mode, executor, decorate(route, initializer, new SendAttachment(route.getPipeline())), true);
    }
    /** Strings: */
    if (CharSequence.class.isAssignableFrom(type)) {
      return next(mode, executor, decorate(route, initializer, new SendCharSequence(route.getPipeline())), true);
    }
    /** RawByte: */
    if (byte[].class == type) {
      return next(mode, executor, decorate(route, initializer, new SendByteArray(route.getPipeline())), true);
    }
    if (ByteBuffer.class.isAssignableFrom(type)) {
      return next(mode, executor, decorate(route, initializer, new SendByteBuffer(route.getPipeline())), true);
    }

    if (responseHandler != null) {
      return responseHandler.stream().filter(it -> it.matches(returnType))
          .findFirst()
          .map(factory ->
              next(mode, executor, decorate(route, initializer, factory.create(route.getPipeline())), true)
          )
          .orElseGet(
              () -> next(mode, executor, decorate(route, initializer, new DefaultHandler(route.getPipeline())),
                  true));
    }
    return next(mode, executor, decorate(route, initializer, new DefaultHandler(route.getPipeline())), true);
  }

  private static Handler decorate(Route route, ContextInitializer initializer, Handler handler) {
    Handler pipeline = handler;
    if (route.isHttpHead()) {
      pipeline = new HeadResponseHandler(pipeline);
    }
    if (initializer == null) {
      return pipeline;
    }
    return new PostDispatchInitializerHandler(initializer, pipeline);
  }

  private static Handler completableFuture(ExecutionMode mode, Route next, Executor executor, ContextInitializer initializer) {
    return next(mode, executor,
        new DetachHandler(decorate(next, initializer, new CompletionStageHandler(next.getPipeline()))),
        false);
  }

  private static Handler rxFlowable(ExecutionMode mode, Route next, Executor executor, ContextInitializer initializer) {
    return next(mode, executor,
        new DetachHandler(decorate(next, initializer, new RxFlowableHandler(next.getPipeline()))),
        false);
  }

  private static Handler reactivePublisher(ExecutionMode mode, Route next, Executor executor, ContextInitializer initializer) {
    return next(mode, executor,
        new DetachHandler(decorate(next, initializer, new ReactivePublisherHandler(next.getPipeline()))),
        false);
  }

  private static Handler rxDisposable(ExecutionMode mode, Route next, Executor executor, ContextInitializer initializer) {
    return next(mode, executor,
        new DetachHandler(decorate(next, initializer, new SendDirect(next.getPipeline()))),
        false);
  }

  private static Handler rxObservable(ExecutionMode mode, Route next, Executor executor, ContextInitializer initializer) {
    return next(mode, executor,
        new DetachHandler(decorate(next, initializer, new ObservableHandler(next.getPipeline()))),
        false);
  }

  private static Handler reactorFlux(ExecutionMode mode, Route next, Executor executor, ContextInitializer initializer) {
    return next(mode, executor,
        new DetachHandler(decorate(next, initializer, new ReactorFluxHandler(next.getPipeline()))),
        false);
  }

  private static Handler reactorMono(ExecutionMode mode, Route next, Executor executor, ContextInitializer initializer) {
    return next(mode, executor,
        new DetachHandler(decorate(next, initializer, new ReactorMonoHandler(next.getPipeline()))),
        false);
  }

  private static Handler kotlinJob(ExecutionMode mode, Route next, Executor executor, ContextInitializer initializer) {
    return next(mode, executor, new DetachHandler(new KotlinJobHandler(next.getPipeline())),
        false);
  }

  private static Handler kotlinContinuation(ExecutionMode mode, Route next, Executor executor, ContextInitializer initializer) {
    return next(mode, executor, new DetachHandler(decorate(next, initializer, next.getPipeline())), false);
  }

  private static Handler single(ExecutionMode mode, Route next, Executor executor, ContextInitializer initializer) {
    return next(mode, executor,
        new DetachHandler(decorate(next, initializer, new RxSingleHandler(next.getPipeline()))),
        false);
  }

  private static Handler rxMaybe(ExecutionMode mode, Route next, Executor executor, ContextInitializer initializer) {
    return next(mode, executor,
        new DetachHandler(decorate(next, initializer, new RxMaybeHandler(next.getPipeline()))),
        false);
  }

  private static Handler next(ExecutionMode mode, Executor executor, Handler handler,
      boolean blocking) {
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

  private static Optional<Class> loadClass(ClassLoader loader, String name) {
    try {
      return Optional.of(loader.loadClass(name));
    } catch (ClassNotFoundException x) {
      return Optional.empty();
    }
  }
}
