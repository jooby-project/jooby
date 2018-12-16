package io.jooby.internal;

import io.jooby.ExecutionMode;
import io.jooby.Renderer;
import io.jooby.Route;
import io.jooby.internal.handler.CompletionStageHandler;
import io.jooby.internal.handler.DefaultHandler;
import io.jooby.internal.handler.DetachHandler;
import io.jooby.internal.handler.ChainedHandler;
import io.jooby.internal.handler.WorkerExecHandler;
import io.jooby.internal.handler.MaybeHandler;
import io.jooby.internal.handler.PublisherHandler;
import io.jooby.internal.handler.SingleHandler;
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
    ChainedHandler pipeline = pipeline(route(String.class, h), ExecutionMode.EVENT_LOOP);
    assertTrue(pipeline instanceof DefaultHandler);
    assertTrue(pipeline.next() == h,
        "found: " + pipeline.next() + ", expected: " + h.getClass());
  }

  @Test
  public void eventLoopAlwaysDispatchToExecutorOnSimpleTypes() {
    Executor executor = task -> {
    };
    Route.Handler h = ctx -> "OK";
    ChainedHandler pipeline = pipeline(route(String.class, h, executor),
        ExecutionMode.EVENT_LOOP);
    assertTrue(pipeline instanceof WorkerExecHandler, "found: " + pipeline);
    Route.Handler next = pipeline.next();
    assertTrue(next instanceof DefaultHandler);
    next = ((DefaultHandler) next).next();
    assertTrue(next == h, "found: " + next + ", expected: " + h.getClass());
  }

  @Test
  public void eventLoopDetachOnCompletableFutures() {
    Route.Handler h = ctx -> "OK";
    ChainedHandler pipeline = pipeline(route(CompletableFuture.class, h),
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
    ChainedHandler pipeline = pipeline(route(CompletableFuture.class, h, executor),
        ExecutionMode.EVENT_LOOP);
    assertTrue(pipeline instanceof WorkerExecHandler, "found: " + pipeline);
    Route.Handler next = pipeline.next();
    assertTrue(next instanceof DetachHandler, "found: " + next);
    next = ((DetachHandler) next).next();
    assertTrue(next instanceof CompletionStageHandler, "found: " + next);
    next = ((CompletionStageHandler) next).next();
    assertTrue(next == h, "found: " + next + ", expected: " + h.getClass());
  }

  @Test
  public void eventLoopDetachOnRx2Single() {
    Route.Handler h = ctx -> "OK";
    ChainedHandler pipeline = pipeline(route(Single.class, h), ExecutionMode.EVENT_LOOP);
    assertTrue(pipeline instanceof DetachHandler);
    Route.Handler next = pipeline.next();
    assertTrue(next instanceof SingleHandler);
    SingleHandler reactive = (SingleHandler) next;
    assertTrue(reactive.next() == h,
        "found: " + reactive.next() + ", expected: " + h.getClass());
  }

  @Test
  public void eventLoopDetachOnPublisher() {
    Route.Handler h = ctx -> "OK";
    ChainedHandler pipeline = pipeline(route(Publisher.class, h), ExecutionMode.EVENT_LOOP);
    assertTrue(pipeline instanceof DetachHandler, "found: " + pipeline);
    Route.Handler next = pipeline.next();
    assertTrue(next instanceof PublisherHandler);
    PublisherHandler reactive = (PublisherHandler) next;
    assertTrue(reactive.next() == h,
        "found: " + reactive.next() + ", expected: " + h.getClass());
  }

  @Test
  public void eventLoopDetachOnFlowable() {
    Route.Handler h = ctx -> "OK";
    ChainedHandler pipeline = pipeline(route(Flowable.class, h), ExecutionMode.EVENT_LOOP);
    assertTrue(pipeline instanceof DetachHandler, "found: " + pipeline);
    Route.Handler next = pipeline.next();
    assertTrue(next instanceof PublisherHandler);
    PublisherHandler reactive = (PublisherHandler) next;
    assertTrue(reactive.next() == h,
        "found: " + reactive.next() + ", expected: " + h.getClass());
  }

  @Test
  public void workerDoesNothingOnSimpleTypes() {
    Route.Handler h = ctx -> "OK";
    ChainedHandler pipeline = pipeline(route(String.class, h), ExecutionMode.WORKER);
    assertTrue(pipeline instanceof WorkerHandler, "found: " + pipeline);
    Route.Handler next = pipeline.next();
    assertTrue(next instanceof DefaultHandler);
    next = ((DefaultHandler) next).next();
    assertTrue(next == h, "found: " + next + ", expected: " + h.getClass());
  }

  @Test
  public void workerDetachOnCompletableFutures() {
    Route.Handler h = ctx -> "OK";
    ChainedHandler pipeline = pipeline(route(CompletableFuture.class, h), ExecutionMode.WORKER);
    assertTrue(pipeline instanceof WorkerHandler, "found: " + pipeline);
    Route.Handler next = pipeline.next();
    assertTrue(next instanceof DetachHandler, "found: " + next.getClass());
    next = ((DetachHandler) next).next();
    assertTrue(next instanceof CompletionStageHandler, "found: " + next.getClass());
    next = ((CompletionStageHandler) next).next();
    assertTrue(next == h, "found: " + next + ", expected: " + h.getClass());
  }

  @Test
  public void workerDetachOnCompletableRxSingle() {
    Route.Handler h = ctx -> "OK";
    ChainedHandler pipeline = pipeline(route(Single.class, h), ExecutionMode.WORKER);
    assertTrue(pipeline instanceof WorkerHandler, "found: " + pipeline);
    Route.Handler next = pipeline.next();
    assertTrue(next instanceof DetachHandler, "found: " + next.getClass());
    next = ((DetachHandler) next).next();
    assertTrue(next instanceof SingleHandler, "found: " + next.getClass());
    next = ((SingleHandler) next).next();
    assertTrue(next == h, "found: " + next + ", expected: " + h.getClass());
  }

  @Test
  public void workerDetachOnCompletableRxMaybe() {
    Route.Handler h = ctx -> "OK";
    ChainedHandler pipeline = pipeline(route(Maybe.class, h), ExecutionMode.WORKER);
    assertTrue(pipeline instanceof WorkerHandler, "found: " + pipeline);
    Route.Handler next = pipeline.next();
    assertTrue(next instanceof DetachHandler, "found: " + next.getClass());
    next = ((DetachHandler) next).next();
    assertTrue(next instanceof MaybeHandler, "found: " + next.getClass());
    next = ((MaybeHandler) next).next();
    assertTrue(next == h, "found: " + next + ", expected: " + h.getClass());
  }

  @Test
  public void workerDetachOnCompletableRxPublisher() {
    Route.Handler h = ctx -> "OK";
    ChainedHandler pipeline = pipeline(route(Publisher.class, h), ExecutionMode.WORKER);
    assertTrue(pipeline instanceof WorkerHandler, "found: " + pipeline);
    Route.Handler next = pipeline.next();
    assertTrue(next instanceof DetachHandler, "found: " + next.getClass());
    next = ((DetachHandler) next).next();
    assertTrue(next instanceof PublisherHandler, "found: " + next.getClass());
    next = ((PublisherHandler) next).next();
    assertTrue(next == h, "found: " + next + ", expected: " + h.getClass());
  }

  @Test
  public void workerAlwaysDispatchToExecutorOnSimpleTypes() {
    Executor executor = task -> {
    };
    Route.Handler h = ctx -> "OK";
    ChainedHandler pipeline = pipeline(route(String.class, h, executor), ExecutionMode.WORKER);
    assertTrue(pipeline instanceof WorkerExecHandler, "found: " + pipeline);
    Route.Handler next = pipeline.next();
    assertTrue(next instanceof DefaultHandler);
    next = ((DefaultHandler) next).next();
    assertTrue(next == h, "found: " + next + ", expected: " + h.getClass());
  }

  @Test
  public void workerAlwaysDispatchToExecutorOnReactiveType() {
    Executor executor = task -> {
    };
    Route.Handler h = ctx -> "OK";
    ChainedHandler pipeline = pipeline(route(CompletableFuture.class, h, executor),
        ExecutionMode.WORKER);
    assertTrue(pipeline instanceof WorkerExecHandler, "found: " + pipeline);
    Route.Handler next = pipeline.next();
    assertTrue(next instanceof DetachHandler, "found: " + next);
    next = ((DetachHandler) next).next();
    assertTrue(next instanceof CompletionStageHandler, "found: " + next);
    next = ((CompletionStageHandler) next).next();
    assertTrue(next == h, "found: " + next + ", expected: " + h.getClass());
  }

  private ChainedHandler pipeline(RouteImpl route, ExecutionMode mode) {
    return (ChainedHandler) Pipeline.compute(getClass().getClassLoader(), route, mode);
  }

  private RouteImpl route(Type returnType, Route.Handler handler) {
    return route(returnType, handler, null);
  }

  private RouteImpl route(Type returnType, Route.Handler handler, Executor executor) {
    return new RouteImpl("GET", "/", Collections.emptyList(), returnType, handler, handler,
        Renderer.TO_STRING).executor(executor);
  }
}
