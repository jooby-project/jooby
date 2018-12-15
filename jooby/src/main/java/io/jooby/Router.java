/**
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *    Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Predicate;

public interface Router {

  interface Match {
    boolean matches();

    Route route();

    Map<String, String> params();
  }

  /** HTTP Methods: */
  String GET = "GET";
  String POST = "POST";
  String PUT = "PUT";
  String DELETE = "DELETE";
  String PATCH = "PATCH";
  String HEAD = "HEAD";
  String CONNECT = "CONNECT";
  String OPTIONS = "OPTIONS";
  String TRACE = "TRACE";

  /** HTTP Methods: */
  List<String> METHODS = List.of(GET, POST, PUT, DELETE, PATCH, HEAD, CONNECT, OPTIONS, TRACE);

  @Nonnull Router basePath(@Nonnull String basePath);

  @Nonnull String basePath();

  @Nonnull Router use(@Nonnull Predicate<Context> predicate, @Nonnull Router router);

  @Nonnull Router use(@Nonnull String path, @Nonnull Router router);

  @Nonnull Router use(@Nonnull Router router);

  @Nonnull List<Route> routes();

  @Nonnull Router renderer(@Nonnull Renderer renderer);

  @Nonnull Router renderer(@Nonnull String contentType, @Nonnull Renderer renderer);

  @Nonnull Path tmpdir();

  @Nonnull default Router parser(@Nonnull String contentType, @Nonnull Parser parser) {
    return decorate(next -> ctx -> {
      ctx.parser(contentType, parser);
      return next.apply(ctx);
    });
  }

  @Nonnull default Router converter(@Nonnull Converter converter) {
    parser(converter.contentType(), converter);
    renderer(converter);
    return this;
  }

  @Nonnull Executor worker();

  @Nonnull Router worker(Executor worker);

  @Nonnull Router decorate(@Nonnull Route.Decorator decorator);

  @Nonnull Router gzip(@Nonnull Runnable action);

  @Nonnull Router before(@Nonnull Route.Before before);

  @Nonnull Router after(@Nonnull Route.After after);

  @Nonnull Router group(@Nonnull Runnable action);

  @Nonnull Router group(@Nonnull String pattern, @Nonnull Runnable action);

  @Nonnull Router group(@Nonnull Executor executor, @Nonnull Runnable action);

  @Nonnull Router group(@Nonnull Executor executor, @Nonnull String pattern, @Nonnull Runnable action);

  @Nonnull default Route get(@Nonnull String pattern, @Nonnull Route.Handler handler) {
    return route(GET, pattern, handler);
  }

  @Nonnull default Route post(@Nonnull String pattern, @Nonnull Route.Handler handler) {
    return route(POST, pattern, handler);
  }

  @Nonnull default Route put(@Nonnull String pattern, @Nonnull Route.Handler handler) {
    return route(PUT, pattern, handler);
  }

  @Nonnull default Route delete(@Nonnull String pattern, @Nonnull Route.Handler handler) {
    return route(DELETE, pattern, handler);
  }

  @Nonnull default Route patch(@Nonnull String pattern, @Nonnull Route.Handler handler) {
    return route(PATCH, pattern, handler);
  }

  @Nonnull default Route head(@Nonnull String pattern, @Nonnull Route.Handler handler) {
    return route(HEAD, pattern, handler);
  }

  @Nonnull default Route connect(@Nonnull String pattern, @Nonnull Route.Handler handler) {
    return route(CONNECT, pattern, handler);
  }

  @Nonnull default Route options(@Nonnull String pattern, @Nonnull Route.Handler handler) {
    return route(OPTIONS, pattern, handler);
  }

  @Nonnull default Route trace(@Nonnull String pattern, @Nonnull Route.Handler handler) {
    return route(TRACE, pattern, handler);
  }

  @Nonnull Route route(@Nonnull String method, @Nonnull String pattern, @Nonnull
      Route.Handler handler);

  /**
   * Find a matching route using the method name and path. Please note that method name must be in
   * uppercase (GET, POST, etc.).
   *
   * If no match exists this method returns a route with a <code>404</code> handler.
   * See {@link Route.Handler#NOT_FOUND}.
   *
   * @param ctx Web Context.
   * @return A route.
   */
  @Nonnull Match match(@Nonnull Context ctx);

  /** Error handler: */
  @Nonnull Router errorCode(@Nonnull Class<? extends Throwable> type,
      @Nonnull StatusCode statusCode);

  @Nonnull
  default Router error(@Nonnull StatusCode statusCode, @Nonnull Route.ErrorHandler handler) {
    return error(statusCode::equals, handler);
  }

  @Nonnull
  default Router error(@Nonnull Class<? extends Throwable> type,
      @Nonnull Route.ErrorHandler handler) {
    return error((ctx, x, statusCode) -> {
      if (type.isInstance(x) || type.isInstance(x.getCause())) {
        handler.apply(ctx, x, statusCode);
      }
    });
  }

  @Nonnull
  default Router error(@Nonnull Predicate<StatusCode> predicate,
      @Nonnull Route.ErrorHandler handler) {
    return error((ctx, x, statusCode) -> {
      if (predicate.test(statusCode)) {
        handler.apply(ctx, x, statusCode);
      }
    });
  }

  @Nonnull Router error(@Nonnull Route.ErrorHandler handler);

  @Nonnull Route.RootErrorHandler errorHandler();

}
