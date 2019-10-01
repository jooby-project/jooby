/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Utility class that allows us to execute routes using a {@link MockContext}.
 *
 * App.java
 * <pre>{@code
 * {
 *
 *   get("/", ctx -> "OK");
 *
 * }
 * }</pre>
 *
 * UnitTest:
 * <pre>{@code
 *   MockRouter router = new MockRouter(new App());
 *
 *   assertEquals("OK", router.get("/"));
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

    @Nonnull @Override public Object value() {
      return value;
    }
  }

  private static final Consumer NOOP = value -> {
  };

  private Supplier<Jooby> supplier;

  private boolean fullExecution;

  /**
   * Creates a new mock router.
   *
   * @param application Source application.
   */
  public MockRouter(@Nonnull Jooby application) {
    this.supplier = () -> application;
  }

  /**
   * Execute a GET request to the target application.
   *
   * @param path Path to match. Might includes the queryString.
   * @return Route response.
   */
  @Nonnull public MockValue get(@Nonnull String path) {
    return get(path, new MockContext());
  }

  /**
   * Execute a GET request and find a websocket, perform the upgrade and produces a websocket client.
   *
   * App:
   * <pre>{@code
   *   ws("/path", (ctx, initializer) -> {
   *     initializer.onConnect(ws -> {
   *       ws.send("OnConnect");
   *     });
   *   });
   * }</pre>
   *
   * Test:
   * <pre>{@code
   *   MockRouter router = new MockRouter(new App());
   *   router.ws("/path", ws -> {
   *
   *     ws.onMessage(message -> {
   *       System.out.println("Got: " + message);
   *     });
   *
   *     ws.send("Another message");
   *   })
   * }</pre>
   *
   *
   * @param path Path to match.
   * @param callback Websocket client callback.
   * @return Web socket client.
   */
  public MockWebSocketClient ws(@Nonnull String path, Consumer<MockWebSocketClient> callback) {
    MockValue value = get(path, new MockContext());
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

  /**
   * Execute a GET request to the target application.
   *
   * @param path Path to match. Might includes the queryString.
   * @param context Context to use.
   * @return Route response.
   */
  @Nonnull public MockValue get(@Nonnull String path, @Nonnull Context context) {
    return call(Router.GET, path, context);
  }

  /**
   * Execute a GET request to the target application.
   *
   * @param path Path to match. Might includes the queryString.
   * @param consumer Response metadata callback.
   * @return Route response.
   */
  public MockValue get(@Nonnull String path, @Nonnull Consumer<MockResponse> consumer) {
    return get(path, new MockContext(), consumer);
  }

  /**
   * Execute a GET request to the target application.
   *
   * @param path Path to match. Might includes the queryString.
   * @param context Context to use.
   * @param consumer Response metadata callback.
   * @return Route response.
   */
  public MockValue get(@Nonnull String path, @Nonnull MockContext context,
      @Nonnull Consumer<MockResponse> consumer) {
    return call(Router.GET, path, context, consumer);
  }

  /**
   * Execute a POST request to the target application.
   *
   * @param path Path to match. Might includes the queryString.
   * @return Route response.
   */
  public MockValue post(@Nonnull String path) {
    return post(path, new MockContext());
  }

  /**
   * Execute a POST request to the target application.
   *
   * @param path Path to match. Might includes the queryString.
   * @param context Context to use.
   * @return Route response.
   */
  @Nonnull public MockValue post(@Nonnull String path, @Nonnull Context context) {
    return call(Router.POST, path, context);
  }

  /**
   * Execute a POST request to the target application.
   *
   * @param path Path to match. Might includes the queryString.
   * @param consumer Response metadata callback.
   * @return Route response.
   */
  public MockValue post(@Nonnull String path, @Nonnull Consumer<MockResponse> consumer) {
    return post(path, new MockContext(), consumer);
  }

  /**
   * Execute a POST request to the target application.
   *
   * @param path Path to match. Might includes the queryString.
   * @param context Context to use.
   * @param consumer Response metadata callback.
   * @return Route response.
   */
  public MockValue post(@Nonnull String path, @Nonnull MockContext context,
      @Nonnull Consumer<MockResponse> consumer) {
    return call(Router.POST, path, context, consumer);
  }

  /**
   * Execute a DELETE request to the target application.
   *
   * @param path Path to match. Might includes the queryString.
   * @return Route response.
   */
  public MockValue delete(@Nonnull String path) {
    return delete(path, new MockContext());
  }

  /**
   * Execute a DELETE request to the target application.
   *
   * @param path Path to match. Might includes the queryString.
   * @param context Context to use.
   * @return Route response.
   */
  @Nonnull public MockValue delete(@Nonnull String path, @Nonnull Context context) {
    return call(Router.DELETE, path, context);
  }

  /**
   * Execute a DELETE request to the target application.
   *
   * @param path Path to match. Might includes the queryString.
   * @param consumer Response metadata callback.
   * @return Route response.
   */
  public MockValue delete(@Nonnull String path, @Nonnull Consumer<MockResponse> consumer) {
    return delete(path, new MockContext(), consumer);
  }

  /**
   * Execute a DELETE request to the target application.
   *
   * @param path Path to match. Might includes the queryString.
   * @param context Context to use.
   * @param consumer Response metadata callback.
   * @return Route response.
   */
  public MockValue delete(@Nonnull String path, @Nonnull MockContext context,
      @Nonnull Consumer<MockResponse> consumer) {
    return call(Router.DELETE, path, context, consumer);
  }

  /**
   * Execute a PUT request to the target application.
   *
   * @param path Path to match. Might includes the queryString.
   * @return Route response.
   */
  public MockValue put(@Nonnull String path) {
    return put(path, new MockContext());
  }

  /**
   * Execute a PUT request to the target application.
   *
   * @param path Path to match. Might includes the queryString.
   * @param context Context to use.
   * @return Route response.
   */
  @Nonnull public MockValue put(@Nonnull String path, @Nonnull Context context) {
    return call(Router.PUT, path, context);
  }

  /**
   * Execute a PUT request to the target application.
   *
   * @param path Path to match. Might includes the queryString.
   * @param consumer Response metadata callback.
   * @return Route response.
   */
  public MockValue put(@Nonnull String path, @Nonnull Consumer<MockResponse> consumer) {
    return put(path, new MockContext(), consumer);
  }

  /**
   * Execute a PUT request to the target application.
   *
   * @param path Path to match. Might includes the queryString.
   * @param context Context to use.
   * @param consumer Response metadata callback.
   * @return Route response.
   */
  public MockValue put(@Nonnull String path, @Nonnull MockContext context,
      @Nonnull Consumer<MockResponse> consumer) {
    return call(Router.PUT, path, context, consumer);
  }

  /**
   * Execute a PATCH request to the target application.
   *
   * @param path Path to match. Might includes the queryString.
   * @return Route response.
   */
  public MockValue patch(@Nonnull String path) {
    return patch(path, new MockContext());
  }

  /**
   * Execute a PATCH request to the target application.
   *
   * @param path Path to match. Might includes the queryString.
   * @param context Context to use.
   * @return Route response.
   */
  @Nonnull public MockValue patch(@Nonnull String path, @Nonnull Context context) {
    return call(Router.PATCH, path, context);
  }

  /**
   * Execute a PATCH request to the target application.
   *
   * @param path Path to match. Might includes the queryString.
   * @param consumer Response metadata callback.
   * @return Route response.
   */
  public MockValue patch(@Nonnull String path, @Nonnull Consumer<MockResponse> consumer) {
    return patch(path, new MockContext(), consumer);
  }

  /**
   * Execute a PATCH request to the target application.
   *
   * @param path Path to match. Might includes the queryString.
   * @param context Context to use.
   * @param consumer Response metadata callback.
   * @return Route response.
   */
  public MockValue patch(@Nonnull String path, @Nonnull MockContext context,
      @Nonnull Consumer<MockResponse> consumer) {
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
  public MockValue call(@Nonnull String method, @Nonnull String path,
      @Nonnull Context context) {
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
  public MockValue call(@Nonnull String method, @Nonnull String path,
      @Nonnull Consumer<MockResponse> consumer) {
    return call(method, path, new MockContext(), consumer);
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
  public MockValue call(@Nonnull String method, @Nonnull String path, @Nonnull MockContext ctx,
      @Nonnull Consumer<MockResponse> consumer) {
    return call(supplier.get(), method, path, ctx, consumer);
  }

  /**
   * Set whenever to execute the entire pipeline (decorators + handler) or just the handler.
   * This flag is off by default, so only the handlers is executed.
   *
   * @param enabled True for enabled the entire pipeline.
   * @return This mock router.
   */
  public MockRouter setFullExecution(boolean enabled) {
    this.fullExecution = enabled;
    return this;
  }

  private MockValue call(Jooby router, String method, String path, Context ctx,
      Consumer<MockResponse> consumer) {
    MockContext findContext = ctx instanceof MockContext ? (MockContext) ctx : new MockContext();
    findContext.setMethod(method.toUpperCase());
    findContext.setPathString(path);
    findContext.setRouter(router);

    Router.Match match = router.match(findContext);
    Route route = match.route();
    findContext.setPathMap(match.pathMap());
    findContext.setRoute(route);
    Object value;
    try {
      Route.Handler handler = fullExecution ? route.getPipeline() : route.getHandler();
      if (route.getMethod().equals(Router.WS)) {
        WebSocket.Initializer initializer = (WebSocket.Initializer) route.getHandle();
        MockWebSocketConfigurer configurer = new MockWebSocketConfigurer(ctx, initializer);
        return new SingleMockValue(configurer);
      } else {
        value = handler.apply(ctx);
        if (ctx instanceof MockContext) {
          MockResponse response = ((MockContext) ctx).getResponse();
          if (!(value instanceof Context)) {
            response.setResult(value);
          }
          if (response.getContentLength() <= 0) {
            response.setContentLength(contentLength(value));
          }
          consumer.accept(response);
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
}
