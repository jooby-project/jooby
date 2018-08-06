package io.jooby;

import io.jooby.internal.RouterImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.concurrent.Executor;

public class App implements Router {

  private final RouterImpl router = new RouterImpl();

  private Server server;

  private Mode mode = Mode.WORKER;

  @Nonnull public App use(Server server) {
    this.server = server;
    return this;
  }

  @Nonnull @Override public Router error(@Nonnull Route.ErrorHandler handler) {
    return router.error(handler);
  }

  @Nonnull @Override public Router filter(@Nonnull Route.Filter filter) {
    return router.filter(filter);
  }

  @Nonnull @Override public Router before(@Nonnull Route.Before before) {
    return router.before(before);
  }

  @Nonnull @Override public Router after(@Nonnull Route.After after) {
    return router.filter(after);
  }

  @Nonnull @Override public Router renderer(@Nonnull Renderer renderer) {
    return router.renderer(renderer);
  }

  @Nonnull @Override public Router dispatch(@Nonnull Runnable action) {
    return router.dispatch(action);
  }

  @Nonnull @Override public Router dispatch(@Nonnull Executor executor, @Nonnull Runnable action) {
    return router.dispatch(executor, action);
  }

  @Nonnull @Override public Router group(@Nonnull Runnable action) {
    return router.group(action);
  }

  @Nonnull @Override public Router path(@Nonnull String pattern, @Nonnull Runnable action) {
    return router.path(pattern, action);
  }

  @Nonnull @Override
  public Route route(@Nonnull String method, @Nonnull String pattern, @Nonnull Route.Handler handler) {
    return router.route(method, pattern, handler);
  }

  @Nonnull @Override public Route match(@Nonnull String method, @Nonnull String path) {
    return router.match(method, path);
  }

  @Nonnull @Override public Route.RootHandler asRootHandler(@Nonnull Route.Handler handler) {
    return router.asRootHandler(handler);
  }

  /** Error handler: */
  @Nonnull @Override
  public Router errorCode(@Nonnull Class<? extends Throwable> type,
      @Nonnull StatusCode statusCode) {
    return router.errorCode(type, statusCode);
  }

  @Nonnull @Override public StatusCode errorCode(@Nonnull Throwable x) {
    return router.errorCode(x);
  }

  @Nonnull public App mode(@Nonnull Mode mode) {
    this.mode = mode;
    return this;
  }

  /** Log: */
  @Nonnull @Override public Logger log() {
    return LoggerFactory.getLogger(getClass());
  }

  /** Boot: */

  public void start() {
    start(true);
  }

  public void start(boolean join) {
    router.start();
    server
        .mode(mode)
        .start(router);

    Logger log = LoggerFactory.getLogger(getClass());

    log.info("[@{}@{}] {}\n{}\n\nhttp://localhost:{}\n", mode.name().toLowerCase(),
        server.getClass().getSimpleName().toLowerCase(), getClass().getSimpleName(), router,
        server.port());

    if (join) {
      try {
        Thread.currentThread().join();
      } catch (InterruptedException x) {
        Thread.currentThread().interrupt();
      }
    }
  }

  public void stop() {
    server.stop();
  }
}
