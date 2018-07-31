package io.jooby.internal.jetty;

import io.jooby.Context;
import io.jooby.Handler;
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
import java.nio.charset.Charset;
import java.util.concurrent.Executor;

public class JettyContext implements Context {
  private final Request request;
  private final ThreadPool executor;

  public JettyContext(Request request, ThreadPool threadPool) {
    this.request = request;
    this.executor = threadPool;
  }

  @Nonnull @Override public String method() {
    return request.getMethod();
  }

  @Nonnull @Override public String path() {
    return request.getPathInfo();
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
