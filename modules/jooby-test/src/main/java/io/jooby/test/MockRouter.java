/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.Jooby;
import io.jooby.Route;
import io.jooby.Router;
import io.jooby.SneakyThrows;
import io.jooby.WebSocket;

/**
 * Utility class that allows us to execute routes using a {@link MockContext}.
 *
 * <p>App.java
 *
 * <pre>{@code
 * {
 *
 *   get("/", ctx -> "OK");
 *
 * }
 * }</pre>
 *
 * UnitTest:
 *
 * <pre>{@code
 * MockRouter router = new MockRouter(new App());
 *
 * assertEquals("OK", router.get("/"));
 * }</pre>
 *
 * @author edgar
 * @since 2.0.0
 */
public class MockRouter {

  private static class SingleMockValue implements MockValue {
    private final Object value;

    SingleMockValue(Object value) {
      this.value = value;
    }

    @NonNull @Override
    public Object value() {
      return value;
    }
  }

  private static final Consumer NOOP = value -> {};

  private Executor worker;

  private Supplier<Jooby> supplier;

  private boolean fullExecution;

  private MockSession session;

  /**
   * Creates a new mock router.
   *
   * @param application Source application.
   */
  public MockRouter(@NonNull Jooby application) {
    this.supplier = () -> application;
  }

  /**
   * Set a global session. So all route invocations are going to shared the same session as long as
   * they don't use a custom context per invocation.
   *
   * @param session Global session.
   * @return This router.
   */
  public @NonNull MockRouter setSession(@NonNull MockSession session) {
    this.session = session;
    return this;
  }

  /**
   * Get the worker executor for running test.
   *
   * @return Worker executor or <code>null</code>.
   */
  public Executor getWorker() {
    return worker;
  }

  /**
   * Set the worker executor to use.
   *
   * @param worker Worker executor.
   * @return This router.
   */
  public MockRouter setWorker(@NonNull Executor worker) {
    this.worker = worker;
    return this;
  }

  /**
   * Execute a GET request to the target application.
   *
   * @param path Path to match. Might includes the queryString.
   * @return Route response.
   */
  @NonNull public MockValue get(@NonNull String path) {
    return get(path, newContext());
  }

  /**
   * Execute a GET request and find a websocket, perform the upgrade and produces a websocket
   * client.
   *
   * <p>App:
   *
   * <pre>{@code
   * ws("/path", (ctx, initializer) -> {
   *   initializer.onConnect(ws -> {
   *     ws.send("OnConnect");
   *   });
   * });
   * }</pre>
   *
   * Test:
   *
   * <pre>{@code
   * MockRouter router = new MockRouter(new App());
   * router.ws("/path", ws -> {
   *
   *   ws.onMessage(message -> {
   *     System.out.println("Got: " + message);
   *   });
   *
   *   ws.send("Another message");
   * })
   * }</pre>
   *
   * @param path Path to match.
   * @param callback Websocket client callback.
   * @return Web socket client.
   */
  public MockWebSocketClient ws(@NonNull String path, Consumer<MockWebSocketClient> callback) {
    MockValue value = get(path, newContext());
    if (value.value() instanceof MockWebSocketConfigurer) {
      MockWebSocketConfigurer configurer = value.value(MockWebSocketConfigurer.class);
      MockWebSocketClient client = configurer.getClient();
      configurer.ready();
      callback.accept(client);
      client.init();
      return client;
    } else {
      throw new IllegalArgumentException("No websocket fount at: " + path);
    }
  }

  private MockContext newContext() {
    MockContext ctx = new MockContext();
    if (session != null) {
      new MockSession(ctx, session);
    }
    return ctx;
  }

  /**
   * Execute a GET request to the target application.
   *
   * @param path Path to match. Might includes the queryString.
   * @param context Context to use.
   * @return Route response.
   */
  @NonNull public MockValue get(@NonNull String path, @NonNull Context context) {
    return call(Router.GET, path, context);
  }

  /**
   * Execute a GET request to the target application.
   *
   * @param path Path to match. Might includes the queryString.
   * @param consumer Response metadata callback.
   * @return Route response.
   */
  public MockValue get(@NonNull String path, @NonNull Consumer<MockResponse> consumer) {
    return get(path, newContext(), consumer);
  }

  /**
   * Execute a GET request to the target application.
   *
   * @param path Path to match. Might includes the queryString.
   * @param context Context to use.
   * @param consumer Response metadata callback.
   * @return Route response.
   */
  public MockValue get(
      @NonNull String path,
      @NonNull MockContext context,
      @NonNull Consumer<MockResponse> consumer) {
    return call(Router.GET, path, context, consumer);
  }

  /**
   * Execute a POST request to the target application.
   *
   * @param path Path to match. Might includes the queryString.
   * @return Route response.
   */
  public MockValue post(@NonNull String path) {
    return post(path, newContext());
  }

  /**
   * Execute a POST request to the target application.
   *
   * @param path Path to match. Might includes the queryString.
   * @param context Context to use.
   * @return Route response.
   */
  @NonNull public MockValue post(@NonNull String path, @NonNull Context context) {
    return call(Router.POST, path, context);
  }

  /**
   * Execute a POST request to the target application.
   *
   * @param path Path to match. Might includes the queryString.
   * @param consumer Response metadata callback.
   * @return Route response.
   */
  public MockValue post(@NonNull String path, @NonNull Consumer<MockResponse> consumer) {
    return post(path, newContext(), consumer);
  }

  /**
   * Execute a POST request to the target application.
   *
   * @param path Path to match. Might includes the queryString.
   * @param context Context to use.
   * @param consumer Response metadata callback.
   * @return Route response.
   */
  public MockValue post(
      @NonNull String path,
      @NonNull MockContext context,
      @NonNull Consumer<MockResponse> consumer) {
    return call(Router.POST, path, context, consumer);
  }

  /**
   * Execute a DELETE request to the target application.
   *
   * @param path Path to match. Might includes the queryString.
   * @return Route response.
   */
  public MockValue delete(@NonNull String path) {
    return delete(path, newContext());
  }

  /**
   * Execute a DELETE request to the target application.
   *
   * @param path Path to match. Might includes the queryString.
   * @param context Context to use.
   * @return Route response.
   */
  @NonNull public MockValue delete(@NonNull String path, @NonNull Context context) {
    return call(Router.DELETE, path, context);
  }

  /**
   * Execute a DELETE request to the target application.
   *
   * @param path Path to match. Might includes the queryString.
   * @param consumer Response metadata callback.
   * @return Route response.
   */
  public MockValue delete(@NonNull String path, @NonNull Consumer<MockResponse> consumer) {
    return delete(path, newContext(), consumer);
  }

  /**
   * Execute a DELETE request to the target application.
   *
   * @param path Path to match. Might includes the queryString.
   * @param context Context to use.
   * @param consumer Response metadata callback.
   * @return Route response.
   */
  public MockValue delete(
      @NonNull String path,
      @NonNull MockContext context,
      @NonNull Consumer<MockResponse> consumer) {
    return call(Router.DELETE, path, context, consumer);
  }

  /**
   * Execute a PUT request to the target application.
   *
   * @param path Path to match. Might includes the queryString.
   * @return Route response.
   */
  public MockValue put(@NonNull String path) {
    return put(path, newContext());
  }

  /**
   * Execute a PUT request to the target application.
   *
   * @param path Path to match. Might includes the queryString.
   * @param context Context to use.
   * @return Route response.
   */
  @NonNull public MockValue put(@NonNull String path, @NonNull Context context) {
    return call(Router.PUT, path, context);
  }

  /**
   * Execute a PUT request to the target application.
   *
   * @param path Path to match. Might includes the queryString.
   * @param consumer Response metadata callback.
   * @return Route response.
   */
  public MockValue put(@NonNull String path, @NonNull Consumer<MockResponse> consumer) {
    return put(path, newContext(), consumer);
  }

  /**
   * Execute a PUT request to the target application.
   *
   * @param path Path to match. Might includes the queryString.
   * @param context Context to use.
   * @param consumer Response metadata callback.
   * @return Route response.
   */
  public MockValue put(
      @NonNull String path,
      @NonNull MockContext context,
      @NonNull Consumer<MockResponse> consumer) {
    return call(Router.PUT, path, context, consumer);
  }

  /**
   * Execute a PATCH request to the target application.
   *
   * @param path Path to match. Might includes the queryString.
   * @return Route response.
   */
  public MockValue patch(@NonNull String path) {
    return patch(path, newContext());
  }

  /**
   * Execute a PATCH request to the target application.
   *
   * @param path Path to match. Might includes the queryString.
   * @param context Context to use.
   * @return Route response.
   */
  @NonNull public MockValue patch(@NonNull String path, @NonNull Context context) {
    return call(Router.PATCH, path, context);
  }

  /**
   * Execute a PATCH request to the target application.
   *
   * @param path Path to match. Might includes the queryString.
   * @param consumer Response metadata callback.
   * @return Route response.
   */
  public MockValue patch(@NonNull String path, @NonNull Consumer<MockResponse> consumer) {
    return patch(path, newContext(), consumer);
  }

  /**
   * Execute a PATCH request to the target application.
   *
   * @param path Path to match. Might includes the queryString.
   * @param context Context to use.
   * @param consumer Response metadata callback.
   * @return Route response.
   */
  public MockValue patch(
      @NonNull String path,
      @NonNull MockContext context,
      @NonNull Consumer<MockResponse> consumer) {
    return call(Router.PATCH, path, context, consumer);
  }

  /**
   * Execute a PATCH request to the target application.
   *
   * @param method HTTP method.
   * @param path Path to match. Might includes the queryString.
   * @param context Web context.
   * @return Route response.
   */
  public MockValue call(@NonNull String method, @NonNull String path, @NonNull Context context) {
    return call(supplier.get(), method, path, context, NOOP);
  }

  /**
   * Execute a PATCH request to the target application.
   *
   * @param method HTTP method.
   * @param path Path to match. Might includes the queryString.
   * @param consumer Response metadata callback.
   * @return Route response.
   */
  public MockValue call(
      @NonNull String method, @NonNull String path, @NonNull Consumer<MockResponse> consumer) {
    return call(method, path, newContext(), consumer);
  }

  /**
   * Execute a PATCH request to the target application.
   *
   * @param method HTTP method.
   * @param path Path to match. Might includes the queryString.
   * @param ctx Context to use.
   * @param consumer Response metadata callback.
   * @return Route response.
   */
  public MockValue call(
      @NonNull String method,
      @NonNull String path,
      @NonNull MockContext ctx,
      @NonNull Consumer<MockResponse> consumer) {
    return call(supplier.get(), method, path, ctx, consumer);
  }

  /**
   * Set whenever to execute the entire pipeline (decorators + handler) or just the handler. This
   * flag is off by default, so only the handlers is executed.
   *
   * @param enabled True for enabled the entire pipeline.
   * @return This mock router.
   */
  public MockRouter setFullExecution(boolean enabled) {
    this.fullExecution = enabled;
    return this;
  }

  /**
   * Invoke an error handler that matches the given exception.
   *
   * @param cause Exception type.
   * @param consumer Callback.
   */
  public void tryError(Throwable cause, Consumer<MockResponse> consumer) {
    MockContext ctx = newContext();
    tryError(cause, ctx);
    consumer.accept(ctx.getResponse());
  }

  /**
   * Invoke an error handler that matches the given exception.
   *
   * @param cause Exception type.
   * @param ctx Context.
   */
  public void tryError(Throwable cause, Context ctx) {
    var app = supplier.get();
    MockContext findContext = ctx instanceof MockContext ? (MockContext) ctx : newContext();
    findContext.setRouter(app);
    var handler = app.getErrorHandler();
    handler.apply(ctx, cause, app.errorCode(cause));
  }

  private MockValue call(
      Jooby router, String method, String path, Context ctx, Consumer<MockResponse> consumer) {
    MockContext findContext = ctx instanceof MockContext ? (MockContext) ctx : newContext();
    findContext.setMethod(method.toUpperCase());
    findContext.setRequestPath(path);
    findContext.setRouter(router);
    findContext.setConsumer(consumer);
    findContext.setMockRouter(this);

    Router.Match match = router.match(findContext);
    Route route = match.route();
    boolean isCoroutine = route.getAttribute("coroutine") == Boolean.TRUE;
    if (isCoroutine) {
      router.setWorker(Optional.ofNullable(getWorker()).orElseGet(MockRouter::singleThreadWorker));
    }
    findContext.setPathMap(match.pathMap());
    findContext.setRoute(route);
    Object value;
    try {
      if (route.getMethod().equals(Router.WS)) {
        var initializer = ((WebSocket.Handler) route.getHandler()).getInitializer();
        var configurer = new MockWebSocketConfigurer(ctx, initializer);
        return new SingleMockValue(configurer);
      } else {
        var handler = fullExecution ? route.getPipeline() : route.getHandler();
        value = handler.apply(ctx);
        if (ctx instanceof MockContext) {
          MockResponse response = ((MockContext) ctx).getResponse();
          Object responseValue;
          if (isCoroutine) {
            response.getLatch().await();
            responseValue = response.value();
          } else {
            if (value != null && !(value instanceof Context)) {
              response.setResult(value);
            }
            responseValue = Optional.ofNullable(response.value()).orElse(value);
          }
          if (response.getContentLength() <= 0) {
            response.setContentLength(contentLength(responseValue));
          }
          consumer.accept(response);
          return new SingleMockValue(responseValue);
        }
        return new SingleMockValue(value);
      }
    } catch (Exception x) {
      throw SneakyThrows.propagate(x);
    }
  }

  private long contentLength(Object value) {
    if (value instanceof CharSequence || value instanceof Number || value instanceof Boolean) {
      return value.toString().length();
    }
    if (value instanceof byte[]) {
      return ((byte[]) value).length;
    }
    return -1;
  }

  private static Executor singleThreadWorker() {
    return Executors.newSingleThreadExecutor(
        task -> {
          Thread thread = new Thread(task, "single-thread");
          thread.setDaemon(true);
          return thread;
        });
  }
}
