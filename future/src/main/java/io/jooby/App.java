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
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.Executor;
import java.util.function.Predicate;

public class App implements Router {

  private final RouterImpl router = new RouterImpl();

  private Path tmpdir = Paths.get(System.getProperty("java.io.tmpdir"), appname(getClass()))
      .toAbsolutePath();

  private Mode mode = Mode.WORKER;

  @Nonnull @Override public App basePath(String basePath) {
    router.basePath(basePath);
    return this;
  }

  @Nonnull @Override public String basePath() {
    return router.basePath();
  }

  @Nonnull @Override
  public App use(@Nonnull Router router) {
    this.router.use(router);
    return this;
  }

  @Nonnull @Override
  public App use(@Nonnull Predicate<Context> predicate, @Nonnull Router router) {
    this.router.use(predicate, router);
    return this;
  }

  @Nonnull @Override public App use(@Nonnull String path, @Nonnull Router router) {
    this.router.use(path, router);
    return this;
  }

  @Nonnull @Override public List<Route> routes() {
    return router.routes();
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

  @Nonnull @Override public Match match(@Nonnull Context ctx) {
    return router.match(ctx);
  }

  /** Error handler: */
  @Nonnull @Override
  public Router errorCode(@Nonnull Class<? extends Throwable> type,
      @Nonnull StatusCode statusCode) {
    return router.errorCode(type, statusCode);
  }

  /** Log: */
  public Logger log() {
    return LoggerFactory.getLogger(getClass());
  }

  @Nonnull @Override public Route.RootErrorHandler errorHandler() {
    return router.errorHandler();
  }

  @Nonnull @Override public String defaultContentType() {
    return router.defaultContentType();
  }

  @Nonnull @Override public App defaultContentType(@Nonnull String contentType) {
    router.defaultContentType(contentType);
    return this;
  }

  public Path tmpdir() {
    return tmpdir;
  }

  public App tmpdir(@Nonnull Path tmpdir) {
    this.tmpdir = tmpdir;
    return this;
  }

  public Mode mode() {
    return mode;
  }

  public App mode(Mode mode) {
    this.mode = mode;
    return this;
  }

  /** Boot: */
  public Server start() {
    Server server = ServiceLoader.load(Server.class)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("No server found."));
    server.deploy(this);
    return server.start();
  }

  public App start(Server server) {
    /** Start router: */
    ensureTmpdir(tmpdir);
    Logger log = log();
    router.start(log);
    log.info("{} [{}@{}]\n\n{}\n\nlistening on:\n  http://localhost:{}{}\n",
        getClass().getSimpleName(),
        server.getClass().getSimpleName().toLowerCase(), mode.name().toLowerCase(), router,
        server.port(), router.basePath());
    return this;
  }

  public App stop() {
    router.destroy();
    return this;
  }

  @Override public String toString() {
    return router.toString();
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
