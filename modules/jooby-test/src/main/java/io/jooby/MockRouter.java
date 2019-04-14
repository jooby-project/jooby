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

public class MockRouter {

  private static final Consumer NOOP = value -> {
  };

  private Supplier<Jooby> supplier;

  public MockRouter(Supplier<Jooby> supplier) {
    this.supplier = supplier;
  }

  public MockRouter(Consumer<Jooby> consumer) {
    this.supplier = () -> {
      Jooby jooby = new Jooby();
      Environment env = Environment.loadEnvironment(new EnvironmentOptions()
          .setClassLoader(jooby.getClass().getClassLoader())
          .setActiveNames("test"));
      jooby.setEnvironment(env);
      consumer.accept(jooby);
      return jooby;
    };
  }

  /* **********************************************************************************************
   * GET
   * **********************************************************************************************
   */
  public Object get(@Nonnull String path) {
    return get(path, NOOP, NOOP);
  }

  public Object get(@Nonnull String path, @Nonnull Consumer<MockResponse> consumer) {
    return get(path, NOOP, consumer);
  }

  public Object get(@Nonnull String path, @Nonnull Consumer<MockContext> prepare,
      @Nonnull Consumer<MockResponse> consumer) {
    return route(Router.GET, path, prepare, consumer);
  }

  /* **********************************************************************************************
   * POST
   * **********************************************************************************************
   */
  public Object post(@Nonnull String path) {
    return post(path, NOOP, NOOP);
  }

  public Object post(@Nonnull String path, @Nonnull Consumer<MockResponse> consumer) {
    return post(path, NOOP, consumer);
  }

  public Object post(@Nonnull String path, @Nonnull Consumer<MockContext> prepare,
      @Nonnull Consumer<MockResponse> consumer) {
    return route(Router.POST, path, prepare, consumer);
  }

  /* **********************************************************************************************
   * DELETE
   * **********************************************************************************************
   */

  public Object delete(@Nonnull String path) {
    return delete(path, NOOP, NOOP);
  }

  public Object delete(@Nonnull String path, @Nonnull Consumer<MockResponse> consumer) {
    return delete(path, NOOP, consumer);
  }

  public Object delete(@Nonnull String path, @Nonnull Consumer<MockContext> prepare,
      @Nonnull Consumer<MockResponse> consumer) {
    return route(Router.DELETE, path, prepare, consumer);
  }

  /* **********************************************************************************************
   * Route:
   * **********************************************************************************************
   */
  public Object route(@Nonnull String method, @Nonnull String path,
      @Nonnull Consumer<MockResponse> consumer) {
    return route(method, path, NOOP, consumer);
  }

  public Object route(@Nonnull String method, @Nonnull String path,
      @Nonnull Consumer<MockContext> prepare,
      @Nonnull Consumer<MockResponse> consumer) {
    return route(supplier.get(), method, path, prepare, consumer);
  }

  private Object route(Jooby router, String method, String path, Consumer<MockContext> prepare,
      Consumer<MockResponse> consumer) {
    MockContext ctx = new MockContext()
        .setMethod(method)
        .setPathString(path);
    if (prepare != null) {
      prepare.accept(ctx);
    }
    Router.Match match = router.match(ctx);
    ctx.setPathMap(match.pathMap());
    ctx.setRoute(match.route());
    Object value;
    try {
      value = match.route().getHandler().apply(ctx);
      MockResponse result = new MockResponse(value, ctx.getStatusCode());
      /** Content-Type: */
      result.header("Content-Type",
          ctx.getResponseContentType().toContentTypeHeader(ctx.getResponseCharset()));

      /** Length: */
      long responseLength = ctx.getResponseLength();
      if (responseLength > 0) {
        result.header("Content-Length", Long.toString(responseLength));
      } else {
        result.header("Content-Length", Long.toString(value.toString().length()));
      }
      consumer.accept(result);
      return value;
    } catch (Exception x) {
      MockResponse result = new MockResponse(x, ctx.getStatusCode());
      consumer.accept(result);
      return x;
    }
  }

  public MockRouter apply(Consumer<MockRouter> consumer) {
    consumer.accept(this);
    return this;
  }
}
