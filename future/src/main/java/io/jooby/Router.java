package io.jooby;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.concurrent.Executor;
import java.util.function.Predicate;

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

  @Nonnull Router renderer(@Nonnull Renderer renderer);

  @Nonnull Router filter(@Nonnull Filter filter);

  @Nonnull Router dispatch(@Nonnull Runnable action);

  @Nonnull Router dispatch(@Nonnull Executor executor, @Nonnull Runnable action);

  @Nonnull Router group(@Nonnull Runnable action);

  @Nonnull Router path(@Nonnull String pattern, @Nonnull Runnable action);

  @Nonnull default Route get(@Nonnull String pattern, @Nonnull Handler handler) {
    return route(GET, pattern, handler);
  }

  @Nonnull default Route post(@Nonnull String pattern, @Nonnull Handler handler) {
    return route(POST, pattern, handler);
  }

  @Nonnull default Route put(@Nonnull String pattern, @Nonnull Handler handler) {
    return route(PUT, pattern, handler);
  }

  @Nonnull default Route delete(@Nonnull String pattern, @Nonnull Handler handler) {
    return route(DELETE, pattern, handler);
  }

  @Nonnull default Route patch(@Nonnull String pattern, @Nonnull Handler handler) {
    return route(PATCH, pattern, handler);
  }

  @Nonnull default Route head(@Nonnull String pattern, @Nonnull Handler handler) {
    return route(HEAD, pattern, handler);
  }

  @Nonnull default Route connect(@Nonnull String pattern, @Nonnull Handler handler) {
    return route(CONNECT, pattern, handler);
  }

  @Nonnull default Route options(@Nonnull String pattern, @Nonnull Handler handler) {
    return route(OPTIONS, pattern, handler);
  }

  @Nonnull default Route trace(@Nonnull String pattern, @Nonnull Handler handler) {
    return route(TRACE, pattern, handler);
  }

  @Nonnull Route route(@Nonnull String method, @Nonnull String pattern, @Nonnull Handler handler);

  @Nonnull default RootHandler matchRoot(@Nonnull String method, @Nonnull String path) {
    return asRootHandler(match(method, path));
  }

  @Nonnull Handler match(@Nonnull String method, @Nonnull String path);

  @Nonnull RootHandler asRootHandler(@Nonnull Handler handler);

  /** Error handler: */
  @Nonnull Router errorCode(@Nonnull Class<? extends Throwable> type, @Nonnull StatusCode statusCode);

  @Nonnull StatusCode errorCode(@Nonnull Throwable x);

  @Nonnull default Router error(@Nonnull StatusCode statusCode, @Nonnull ErrorHandler handler) {
    return error(statusCode::equals, handler);
  }

  @Nonnull
  default Router error(@Nonnull Class<? extends Throwable> type, @Nonnull ErrorHandler handler) {
    return error((ctx, x, statusCode) -> {
      if (type.isInstance(x) || type.isInstance(x.getCause())) {
        handler.apply(ctx, x, statusCode);
      }
    });
  }

  @Nonnull
  default Router error(@Nonnull Predicate<StatusCode> predicate, @Nonnull ErrorHandler handler) {
    return error((ctx, x, statusCode) -> {
      if (predicate.test(statusCode)) {
        handler.apply(ctx, x, statusCode);
      }
    });
  }

  @Nonnull Router error(@Nonnull ErrorHandler handler);

  /** Log: */
  @Nonnull default Logger log() {
    return LoggerFactory.getLogger(Router.class);
  }
}
