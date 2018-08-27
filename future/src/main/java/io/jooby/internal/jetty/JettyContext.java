package io.jooby.internal.jetty;

import io.jooby.*;
import org.eclipse.jetty.http.MultiPartFormInputStream;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpOutput;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.MultiMap;
import org.jooby.funzy.Throwing;

import javax.annotation.Nonnull;
import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.jooby.funzy.Throwing.throwingConsumer;

public class JettyContext extends BaseContext {
  private final Request request;
  private final Executor executor;
  private final String target;
  private final Route.RootErrorHandler errorHandler;
  private QueryString query;
  private Form form;
  private Multipart multipart;
  private Consumer<Request> multipartInit;
  private List<Value.Upload> files;
  private Value.Object headers;

  public JettyContext(String target, Request request, Executor threadPool,
      Consumer<Request> multipartInit, Route.RootErrorHandler errorHandler, Route route) {
    super(route);
    this.target = target;
    this.request = request;
    this.executor = threadPool;
    this.multipartInit = multipartInit;
    this.errorHandler = errorHandler;
  }

  @Nonnull @Override public Body body() {
    try {
      return Body.of(request.getInputStream(), request.getContentLengthLong());
    } catch (IOException x) {
      throw Throwing.sneakyThrow(x);
    }
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

  @Nonnull @Override public Form form() {
    if (form == null) {
      form = new Form();
      formParam(request, form);
    }
    return form;
  }

  @Nonnull @Override public Multipart multipart() {
    if (multipart == null) {
      multipart = new Multipart();
      form = multipart;
      multipartInit.accept(request);
      formParam(request, form);
      // Files:
      try {
        Collection<Part> parts = request.getParts();
        for (Part part : parts) {
          String name = part.getName();
          multipart.put(name,
              register(new JettyUpload(name, (MultiPartFormInputStream.MultiPart) part)));
        }
      } catch (IOException | ServletException x) {
        throw Throwing.sneakyThrow(x);
      }
    }
    return multipart;
  }

  @Nonnull @Override public Value headers() {
    if (headers == null) {
      headers = Value.headers();
      Enumeration<String> names = request.getHeaderNames();
      while (names.hasMoreElements()) {
        String name = names.nextElement();
        Enumeration<String> values = request.getHeaders(name);
        while (values.hasMoreElements()) {
          headers.put(name, values.nextElement());
        }
      }
    }
    return headers;
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

  @Nonnull @Override public Context detach(@Nonnull Runnable action) {
    request.startAsync();
    action.run();
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

  @Nonnull @Override public Context sendBytes(@Nonnull byte[] data) {
    byte[] result = (byte[]) fireAfter(data);
    return sendBytes(ByteBuffer.wrap(result));
  }

  @Nonnull @Override public Context sendText(@Nonnull String data, @Nonnull Charset charset) {
    String result = (String) fireAfter(data);
    return sendBytes(ByteBuffer.wrap(result.getBytes(charset)));
  }

  @Nonnull @Override public Context sendBytes(@Nonnull ByteBuffer data) {
    ByteBuffer result = (ByteBuffer) fireAfter(data);
    HttpOutput sender = request.getResponse().getHttpOutput();
    if (request.isAsyncStarted()) {
      AsyncContext asyncContext = request.getAsyncContext();
      sender.sendContent(result, new JettyCallback(asyncContext));
    } else {
      sender.sendContent(result, Callback.NOOP);
    }
    return this;
  }

  @Nonnull @Override public Context sendError(Throwable cause) {
    errorHandler.apply(this, cause);
    return this;
  }

  @Override public boolean isResponseStarted() {
    return request.getResponse().isCommitted();
  }

  @Override public void destroy() {
    if (files != null) {
      // TODO: use a log
      files.forEach(throwingConsumer(Value.Upload::destroy).onFailure(x -> x.printStackTrace()));
    }
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

  private Value.Upload register(Value.Upload upload) {
    if (files == null) {
      files = new ArrayList<>();
    }
    files.add(upload);
    return upload;
  }

  private static void formParam(Request request, Form form) {
    Enumeration<String> names = request.getParameterNames();
    MultiMap<String> query = request.getQueryParameters();
    while (names.hasMoreElements()) {
      String name = names.nextElement();
      if (query == null || !query.containsKey(name)) {
        form.put(name, request.getParameter(name));
      }
    }
  }
}
