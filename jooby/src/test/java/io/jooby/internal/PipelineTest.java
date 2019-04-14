package io.jooby.internal;

import io.jooby.ExecutionMode;
import io.jooby.Renderer;
import io.jooby.Route;
import io.jooby.internal.handler.LinkedHandler;
import io.jooby.internal.handler.SendCharSequence;
import io.jooby.internal.handler.CompletionStageHandler;
import io.jooby.internal.handler.DetachHandler;
import io.jooby.internal.handler.DispatchHandler;
import io.jooby.internal.handler.reactive.ReactivePublisherHandler;
import io.jooby.internal.handler.reactive.RxMaybeHandler;
import io.jooby.internal.handler.reactive.RxFlowableHandler;
import io.jooby.internal.handler.reactive.RxSingleHandler;
import io.jooby.internal.handler.WorkerHandler;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PipelineTest {

  @Test
  public void eventLoopDoesNothingOnSimpleTypes() {
    Route.Handler h = ctx -> "OK";
    LinkedHandler pipeline = (LinkedHandler) pipeline(route(String.class, h),
        ExecutionMode.EVENT_LOOP);
    assertTrue(pipeline instanceof SendCharSequence, pipeline.toString());
    assertTrue(pipeline.next() == h,
        "found: " + pipeline.next() + ", expected: " + h.getClass());
  }

  @Test
  public void eventLoopAlwaysDispatchToExecutorOnSimpleTypes() {
    Executor executor = task -> {
    };
    Route.Handler h = ctx -> "OK";
    LinkedHandler pipeline = (LinkedHandler) pipeline(route(String.class, h),
        ExecutionMode.EVENT_LOOP, executor);
    assertTrue(pipeline instanceof DispatchHandler, "found: " + pipeline);
    Route.Handler next = pipeline.next();
    assertTrue(next instanceof SendCharSequence);
    next = ((LinkedHandler) next).next();
    assertTrue(next == h, "found: " + next + ", expected: " + h.getClass());
  }

  @Test
  public void eventLoopDetachOnCompletableFutures() {
    Route.Handler h = ctx -> "OK";
    LinkedHandler pipeline = (LinkedHandler) pipeline(route(CompletableFuture.class, h),
        ExecutionMode.EVENT_LOOP);
    assertTrue(pipeline instanceof DetachHandler);
    Route.Handler next = pipeline.next();
    assertTrue(next instanceof CompletionStageHandler);
    CompletionStageHandler reactive = (CompletionStageHandler) next;
    assertTrue(reactive.next() == h,
        "found: " + reactive.next() + ", expected: " + h.getClass());
  }

  @Test
  public void eventLoopAlwaysDispatchToExecutorOnReactiveTypes() {
    Executor executor = task -> {
    };
    Route.Handler h = ctx -> "OK";
    LinkedHandler pipeline = (LinkedHandler) pipeline(route(CompletableFuture.class, h),
        ExecutionMode.EVENT_LOOP, executor);
    assertTrue(pipeline instanceof DispatchHandler, "found: " + pipeline);
    Route.Handler next = pipeline.next();
    assertTrue(next instanceof DetachHandler, "found: " + next);
    next = ((LinkedHandler) next).next();
    assertTrue(next instanceof CompletionStageHandler, "found: " + next);
    next = ((LinkedHandler) next).next();
    assertTrue(next == h, "found: " + next + ", expected: " + h.getClass());
  }

  @Test
  public void eventLoopDetachOnRx2Single() {
    Route.Handler h = ctx -> "OK";
    LinkedHandler pipeline = (LinkedHandler) pipeline(route(Single.class, h),
        ExecutionMode.EVENT_LOOP);
    assertTrue(pipeline instanceof DetachHandler);
    Route.Handler next = pipeline.next();
    assertTrue(next instanceof RxSingleHandler);
    RxSingleHandler reactive = (RxSingleHandler) next;
    assertTrue(reactive.next() == h,
        "found: " + reactive.next() + ", expected: " + h.getClass());
  }

  @Test
  public void eventLoopDetachOnPublisher() {
    Route.Handler h = ctx -> "OK";
    LinkedHandler pipeline = (LinkedHandler) pipeline(route(Publisher.class, h),
        ExecutionMode.EVENT_LOOP);
    assertTrue(pipeline instanceof DetachHandler, "found: " + pipeline);
    Route.Handler next = pipeline.next();
    assertTrue(next instanceof ReactivePublisherHandler, next.toString());
    ReactivePublisherHandler reactive = (ReactivePublisherHandler) next;
    assertTrue(reactive.next() == h,
        "found: " + reactive.next() + ", expected: " + h.getClass());
  }

  @Test
  public void eventLoopDetachOnFlowable() {
    Route.Handler h = ctx -> "OK";
    LinkedHandler pipeline = (LinkedHandler) pipeline(route(Flowable.class, h),
        ExecutionMode.EVENT_LOOP);
    assertTrue(pipeline instanceof DetachHandler, "found: " + pipeline);
    Route.Handler next = pipeline.next();
    assertTrue(next instanceof RxFlowableHandler);
    RxFlowableHandler reactive = (RxFlowableHandler) next;
    assertTrue(reactive.next() == h,
        "found: " + reactive.next() + ", expected: " + h.getClass());
  }

  @Test
  public void workerDoesNothingOnSimpleTypes() {
    Route.Handler h = ctx -> "OK";
    LinkedHandler pipeline = (LinkedHandler) pipeline(route(String.class, h), ExecutionMode.WORKER);
    assertTrue(pipeline instanceof WorkerHandler, "found: " + pipeline);
    Route.Handler next = pipeline.next();
    assertTrue(next instanceof SendCharSequence);
    next = ((LinkedHandler) next).next();
    assertTrue(next == h, "found: " + next + ", expected: " + h.getClass());
  }

  @Test
  public void workerDetachOnCompletableFutures() {
    Route.Handler h = ctx -> "OK";
    LinkedHandler pipeline = (LinkedHandler) pipeline(route(CompletableFuture.class, h),
        ExecutionMode.WORKER);
    assertTrue(pipeline instanceof WorkerHandler, "found: " + pipeline);
    Route.Handler next = pipeline.next();
    assertTrue(next instanceof DetachHandler, "found: " + next.getClass());
    next = ((LinkedHandler) next).next();
    assertTrue(next instanceof CompletionStageHandler, "found: " + next.getClass());
    next = ((LinkedHandler) next).next();
    assertTrue(next == h, "found: " + next + ", expected: " + h.getClass());
  }

  @Test
  public void workerDetachOnCompletableRxSingle() {
    Route.Handler h = ctx -> "OK";
    LinkedHandler pipeline = (LinkedHandler) pipeline(route(Single.class, h), ExecutionMode.WORKER);
    assertTrue(pipeline instanceof WorkerHandler, "found: " + pipeline);
    Route.Handler next = pipeline.next();
    assertTrue(next instanceof DetachHandler, "found: " + next.getClass());
    next = ((LinkedHandler) next).next();
    assertTrue(next instanceof RxSingleHandler, "found: " + next.getClass());
    next = ((LinkedHandler) next).next();
    assertTrue(next == h, "found: " + next + ", expected: " + h.getClass());
  }

  @Test
  public void workerDetachOnCompletableRxMaybe() {
    Route.Handler h = ctx -> "OK";
    LinkedHandler pipeline = (LinkedHandler) pipeline(route(Maybe.class, h), ExecutionMode.WORKER);
    assertTrue(pipeline instanceof WorkerHandler, "found: " + pipeline);
    Route.Handler next = pipeline.next();
    assertTrue(next instanceof DetachHandler, "found: " + next.getClass());
    next = ((LinkedHandler) next).next();
    assertTrue(next instanceof RxMaybeHandler, "found: " + next.getClass());
    next = ((LinkedHandler) next).next();
    assertTrue(next == h, "found: " + next + ", expected: " + h.getClass());
  }

  @Test
  public void workerDetachOnCompletableRxPublisher() {
    Route.Handler h = ctx -> "OK";
    LinkedHandler pipeline = (LinkedHandler) pipeline(route(Publisher.class, h),
        ExecutionMode.WORKER);
    assertTrue(pipeline instanceof WorkerHandler, "found: " + pipeline);
    Route.Handler next = pipeline.next();
    assertTrue(next instanceof DetachHandler, "found: " + next.getClass());
    next = ((LinkedHandler) next).next();
    assertTrue(next instanceof ReactivePublisherHandler, "found: " + next.getClass());
    next = ((LinkedHandler) next).next();
    assertTrue(next == h, "found: " + next + ", expected: " + h.getClass());
  }

  @Test
  public void workerAlwaysDispatchToExecutorOnSimpleTypes() {
    Executor executor = task -> {
    };
    Route.Handler h = ctx -> "OK";
    LinkedHandler pipeline = (LinkedHandler) pipeline(route(String.class, h), ExecutionMode.WORKER,
        executor);
    assertTrue(pipeline instanceof DispatchHandler, "found: " + pipeline);
    Route.Handler next = pipeline.next();
    assertTrue(next instanceof SendCharSequence);
    next = ((LinkedHandler) next).next();
    assertTrue(next == h, "found: " + next + ", expected: " + h.getClass());
  }

  @Test
  public void workerAlwaysDispatchToExecutorOnReactiveType() {
    Executor executor = task -> {
    };
    Route.Handler h = ctx -> "OK";
    LinkedHandler pipeline = (LinkedHandler) pipeline(route(CompletableFuture.class, h),
        ExecutionMode.WORKER, executor);
    assertTrue(pipeline instanceof DispatchHandler, "found: " + pipeline);
    Route.Handler next = pipeline.next();
    assertTrue(next instanceof DetachHandler, "found: " + next);
    next = ((LinkedHandler) next).next();
    assertTrue(next instanceof CompletionStageHandler, "found: " + next);
    next = ((LinkedHandler) next).next();
    assertTrue(next == h, "found: " + next + ", expected: " + h.getClass());
  }

  private Route.Handler pipeline(Route route, ExecutionMode mode) {
    return pipeline(route, mode, null);
  }

  private Route.Handler pipeline(Route route, ExecutionMode mode, Executor executor) {
    return Pipeline.compute(getClass().getClassLoader(), route, mode, executor, null);
  }

  private Route route(Type returnType, Route.Handler handler) {
    return new Route("GET", "/", returnType, handler, null, null,
        Renderer.TO_STRING, Collections.emptyMap());
  }
}
