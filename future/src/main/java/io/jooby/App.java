package io.jooby;

import io.jooby.internal.RouterImpl;
import org.jooby.funzy.Throwing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;

public class App implements Router {

  private final RouterImpl router = new RouterImpl();

  private Server server;

  private Mode mode = Mode.WORKER;

  private int port = 8080;

  private Path tmpdir;

  public App() {
    tmpdir = Paths.get(System.getProperty("java.io.tmpdir"), appname(getClass())).toAbsolutePath();
  }

  @Nonnull @Override public Router basePath(String basePath) {
    router.basePath(basePath);
    return this;
  }

  @Nonnull public App use(Server server) {
    this.server = server;
    return this;
  }

  @Nonnull @Override public Router gzip(@Nonnull Runnable action) {
    return router.gzip(action);
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
    return router.after(after);
  }

  @Nonnull @Override public Router renderer(@Nonnull Renderer renderer) {
    return router.renderer(renderer);
  }

  @Nonnull @Override public Route.Handler detach(@Nonnull Route.DetachHandler handler) {
    return router.detach(handler);
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
  public Route route(@Nonnull String method, @Nonnull String pattern,
      @Nonnull Route.Handler handler) {
    return router.route(method, pattern, handler);
  }

  @Nonnull @Override public Route match(@Nonnull String method, @Nonnull String path) {
    return router.match(method, path);
  }

  /** Error handler: */
  @Nonnull @Override
  public Router errorCode(@Nonnull Class<? extends Throwable> type,
      @Nonnull StatusCode statusCode) {
    return router.errorCode(type, statusCode);
  }

  @Nonnull public App mode(@Nonnull Mode mode) {
    this.mode = mode;
    return this;
  }

  /**
   * Application work/temporary directory. Used internally for tasks like file upload, etc...
   *
   * @param tmpdir Work/temporary directory.
   * @return This application.
   */
  public App tmpdir(@Nonnull Path tmpdir) {
    this.tmpdir = tmpdir;
    return this;
  }

  /** Log: */
  public Logger log() {
    return LoggerFactory.getLogger(getClass());
  }

  @Nonnull @Override public Route.RootErrorHandler errorHandler() {
    return router.errorHandler();
  }

  /** Boot: */

  public void start() {
    start(true);
  }

  public void start(boolean join) {
    ensureTmpdir(tmpdir);

    /** Start router: */
    router.start(log());

    /** Start server: */
    server
        .mode(mode)
        .port(port)
        .tmpdir(tmpdir)
        .start(router);

    Logger log = LoggerFactory.getLogger(getClass());

    log.info("[@{}@{}] {}\n{}\n\nhttp://localhost:{}\n", mode.name().toLowerCase(),
        server.getClass().getSimpleName().toLowerCase(), getClass().getSimpleName(), router, port);

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

  private static void ensureTmpdir(Path tmpdir) {
    try {
      if (!Files.exists(tmpdir)) {
        Files.createDirectories(tmpdir);
      }
    } catch (IOException x) {
      throw Throwing.sneakyThrow(x);
    }
  }

  private static String appname(Class<?> clazz) {
    String[] segments = clazz.getName().split("\\.");
    return segments.length == 1 ? segments[0] : segments[segments.length - 2];
  }
}
