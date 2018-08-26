package io.jooby;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Predicate;

import static java.util.Arrays.asList;
import static java.util.Collections.synchronizedList;

public interface Router {

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
  List<String> METHODS = synchronizedList(
      asList(GET, POST, PUT, DELETE, PATCH, HEAD, CONNECT, OPTIONS, TRACE));

  @Nonnull Router renderer(@Nonnull Renderer renderer);

  @Nonnull default Router parser(@Nonnull Parser parser) {
    return filter(next -> ctx -> {
      ctx.parser(parser.contentType(), parser);
      return next.apply(ctx);
    });
  }

  @Nonnull default Router converter(@Nonnull Converter converter) {
    parser(converter);
    renderer(converter);
    return this;
  }

  @Nonnull Router filter(@Nonnull Route.Filter filter);

  @Nonnull Router gzip(@Nonnull Runnable action);

  @Nonnull Router before(@Nonnull Route.Before before);

  @Nonnull Router after(@Nonnull Route.After after);

  @Nonnull Router detach(@Nonnull Runnable action);

  /**
   * Handler who delegates processing of current request to a custom thread. This idiom works as
   * bridge between Jooby and (normally) a reactive library who follows the publish/subscribe
   * programming model.
   *
   * Rx2 example:
   *
   * <pre>{@code
   *
   * class MyApp extends App {
   *   {
   *     get("/rx2", detach(ctx ->
   *       fromCallable(() -> "Hello Rx2!")
   *                   .subscribeOn(Schedulers.io())
   *                   .observeOn(Schedulers.computation())
   *                   .subscribe(ctx::render, ctx:sendError)
   *     ));
   *   }
   * }
   * }</pre>
   *
   * @param handler
   * @return
   */
  @Nonnull Route.Handler detach(@Nonnull Route.Handler handler);

  @Nonnull Router dispatch(@Nonnull Runnable action);

  @Nonnull Router dispatch(@Nonnull Executor executor, @Nonnull Runnable action);

  @Nonnull Router group(@Nonnull Runnable action);

  @Nonnull Router path(@Nonnull String pattern, @Nonnull Runnable action);

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
   * @param method Method in upper case.
   * @param path Path to match.
   * @return A route.
   */
  @Nonnull Route match(@Nonnull String method, @Nonnull String path);

  /** Error handler: */
  @Nonnull Router errorCode(@Nonnull Class<? extends Throwable> type,
      @Nonnull StatusCode statusCode);

  @Nonnull default Router error(@Nonnull StatusCode statusCode, @Nonnull Route.ErrorHandler handler) {
    return error(statusCode::equals, handler);
  }

  @Nonnull
  default Router error(@Nonnull Class<? extends Throwable> type, @Nonnull Route.ErrorHandler handler) {
    return error((ctx, x, statusCode) -> {
      if (type.isInstance(x) || type.isInstance(x.getCause())) {
        handler.apply(ctx, x, statusCode);
      }
    });
  }

  @Nonnull
  default Router error(@Nonnull Predicate<StatusCode> predicate, @Nonnull Route.ErrorHandler handler) {
    return error((ctx, x, statusCode) -> {
      if (predicate.test(statusCode)) {
        handler.apply(ctx, x, statusCode);
      }
    });
  }

  @Nonnull Router error(@Nonnull Route.ErrorHandler handler);

  @Nonnull Route.RootErrorHandler errorHandler();
}
