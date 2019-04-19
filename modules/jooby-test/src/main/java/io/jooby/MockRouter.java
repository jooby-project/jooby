/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
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

  private static final Consumer NOOP = value -> {
  };

  private Supplier<Jooby> supplier;

  private boolean fullExection;

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
  @Nonnull public Object get(@Nonnull String path) {
    return get(path, NOOP);
  }

  /**
   * Execute a GET request to the target application.
   *
   * @param path Path to match. Might includes the queryString.
   * @param consumer Response metadata callback.
   * @return Route response.
   */
  public Object get(@Nonnull String path, @Nonnull Consumer<MockResponse> consumer) {
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
  public Object get(@Nonnull String path, @Nonnull MockContext context,
      @Nonnull Consumer<MockResponse> consumer) {
    return call(Router.GET, path, context, consumer);
  }

  /**
   * Execute a POST request to the target application.
   *
   * @param path Path to match. Might includes the queryString.
   * @return Route response.
   */
  public Object post(@Nonnull String path) {
    return post(path, NOOP);
  }

  /**
   * Execute a POST request to the target application.
   *
   * @param path Path to match. Might includes the queryString.
   * @param consumer Response metadata callback.
   * @return Route response.
   */
  public Object post(@Nonnull String path, @Nonnull Consumer<MockResponse> consumer) {
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
  public Object post(@Nonnull String path, @Nonnull MockContext context,
      @Nonnull Consumer<MockResponse> consumer) {
    return call(Router.POST, path, context, consumer);
  }

  /**
   * Execute a DELETE request to the target application.
   *
   * @param path Path to match. Might includes the queryString.
   * @return Route response.
   */
  public Object delete(@Nonnull String path) {
    return delete(path, NOOP);
  }

  /**
   * Execute a DELETE request to the target application.
   *
   * @param path Path to match. Might includes the queryString.
   * @param consumer Response metadata callback.
   * @return Route response.
   */
  public Object delete(@Nonnull String path, @Nonnull Consumer<MockResponse> consumer) {
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
  public Object delete(@Nonnull String path, @Nonnull MockContext context,
      @Nonnull Consumer<MockResponse> consumer) {
    return call(Router.DELETE, path, context, consumer);
  }

  /**
   * Execute a PUT request to the target application.
   *
   * @param path Path to match. Might includes the queryString.
   * @return Route response.
   */
  public Object put(@Nonnull String path) {
    return put(path, NOOP);
  }

  /**
   * Execute a PUT request to the target application.
   *
   * @param path Path to match. Might includes the queryString.
   * @param consumer Response metadata callback.
   * @return Route response.
   */
  public Object put(@Nonnull String path, @Nonnull Consumer<MockResponse> consumer) {
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
  public Object put(@Nonnull String path, @Nonnull MockContext context,
      @Nonnull Consumer<MockResponse> consumer) {
    return call(Router.PUT, path, context, consumer);
  }

  /**
   * Execute a PATCH request to the target application.
   *
   * @param path Path to match. Might includes the queryString.
   * @return Route response.
   */
  public Object patch(@Nonnull String path) {
    return patch(path, NOOP);
  }

  /**
   * Execute a PATCH request to the target application.
   *
   * @param path Path to match. Might includes the queryString.
   * @param consumer Response metadata callback.
   * @return Route response.
   */
  public Object patch(@Nonnull String path, @Nonnull Consumer<MockResponse> consumer) {
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
  public Object patch(@Nonnull String path, @Nonnull MockContext context,
      @Nonnull Consumer<MockResponse> consumer) {
    return call(Router.PATCH, path, context, consumer);
  }

  /**
   * Execute a PATCH request to the target application.
   *
   * @param method HTTP method.
   * @param path Path to match. Might includes the queryString.
   * @param consumer Response metadata callback.
   * @return Route response.
   */
  public Object call(@Nonnull String method, @Nonnull String path,
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
  public Object call(@Nonnull String method, @Nonnull String path, @Nonnull MockContext ctx,
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
    this.fullExection = enabled;
    return this;
  }

  private Object call(Jooby router, String method, String path, MockContext ctx,
      Consumer<MockResponse> consumer) {
    ctx.setMethod(method.toUpperCase());
    ctx.setPathString(path);
    ctx.setRouter(router);

    Router.Match match = router.match(ctx);
    Route route = match.route();
    ctx.setPathMap(match.pathMap());
    ctx.setRoute(route);
    Object value;
    try {
      Route.Handler handler = fullExection ? route.getPipeline() : route.getHandler();
      value = handler.apply(ctx);
      MockResponse response = ctx.getResponse();
      if (!(value instanceof Context)) {
        response.setResult(value);
      }
      if (response.getContentLength() <= 0) {
        response.setContentLength(contentLength(value));
      }
      consumer.accept(response);
      return value;
    } catch (Exception x) {
      MockResponse result = new MockResponse()
          .setResult(x)
          .setStatusCode(router.errorCode(x));
      consumer.accept(result);
      return x;
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
