package io.jooby.internal.jetty;

import io.jooby.Context;
import io.jooby.Handler;
import io.jooby.Route;
import org.eclipse.jetty.server.HttpOutput;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.jooby.funzy.Throwing;

import javax.annotation.Nonnull;
import javax.servlet.AsyncContext;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.Executor;

public class JettyContext implements Context {
  private final Request request;
  private final ThreadPool executor;
  private final Route route;

  public JettyContext(Request request, ThreadPool threadPool, Route route) {
    this.request = request;
    this.executor = threadPool;
    this.route = route;
  }

  @Nonnull @Override public Route route() {
    return route;
  }

  @Nonnull @Override public String path() {
    return request.getRequestURI();
  }

  @Override public boolean isInIoThread() {
    return false;
  }

  @Nonnull @Override public Executor worker() {
    return executor;
  }

  @Nonnull @Override
  public Context dispatch(@Nonnull Executor executor, @Nonnull Runnable action) {
    AsyncContext ctx = request.startAsync();
    executor.execute(() -> {
      action.run();
      ctx.complete();
    });
    return this;
  }

  @Nonnull @Override public Context statusCode(int statusCode) {
    request.getResponse().setStatus(statusCode);
    return this;
  }

  @Nonnull @Override public Context type(@Nonnull String contentType) {
    return null;
  }

  @Nonnull @Override public Context sendStatusCode(int statusCode) {
    try {
      Response response = request.getResponse();
      response.setLongContentLength(0);
      response.setStatus(statusCode);
      if (!request.isAsyncStarted()) {
        response.closeOutput();
      }
      return this;
    } catch (IOException x) {
      throw Throwing.sneakyThrow(x);
    }
  }

  @Nonnull @Override public Context send(@Nonnull ByteBuffer data) {
    HttpOutput sender = request.getResponse().getHttpOutput();
    sender.sendContent(data, Callback.NOOP);
    return this;
  }

  @Override public boolean isResponseStarted() {
    return request.getResponse().isCommitted();
  }
}
