package io.jooby.internal.jetty;

import io.jooby.Context;
import io.jooby.QueryString;
import io.jooby.Route;
import io.jooby.UrlParser;
import io.jooby.Value;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpOutput;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.UrlEncoded;
import org.jooby.funzy.Throwing;

import javax.annotation.Nonnull;
import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

public class JettyContext implements Context {
  private final Request request;
  private final Executor executor;
  private final Route route;
  private final Map<String, Object> locals = new HashMap<>();
  private final String target;
  private QueryString query;

  public JettyContext(String target, Request request, Executor threadPool, Route route) {
    this.target = target;
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

  @Nonnull @Override public QueryString query() {
    if (query == null) {
      String queryString = request.getQueryString();
      if (queryString == null) {
        query = QueryString.EMPTY;
      } else {
        query = Value.queryString('?' + queryString);
      }
    }
    return query;
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

  @Nonnull @Override public Map<String, Object> locals() {
    return locals;
  }

  @Nonnull @Override public Route.Filter gzip() {
    return next -> ctx -> {
      if (request.getHeader("Accept-Encoding") != null) {
        AtomicReference<Object> holder = new AtomicReference<>();

        /** Gzip: */
        GzipHandler handler = new GzipHandler();
        handler.setHandler(gzipCall(ctx, next, holder));
        handler.handle(target, request, request, request.getResponse());

        /** Check value and rethrow if need it: */
        Object value = holder.get();
        if (value instanceof Exception) {
          throw (Exception) value;
        }
        return value;
      } else {
        return next.apply(ctx);
      }
    };
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

  private static Handler gzipCall(Context ctx, Route.Handler next, AtomicReference<Object> result) {
    return new AbstractHandler() {
      @Override public void handle(String target, Request baseRequest, HttpServletRequest request,
          HttpServletResponse response) {
        try {
          result.set(next.apply(ctx));
        } catch (Throwable x) {
          result.set(x);
        }
      }
    };
  }
}
