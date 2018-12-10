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
package io.jooby.internal.jetty;

import io.jooby.*;
import org.eclipse.jetty.http.MultiPartFormInputStream;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpOutput;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.MultiMap;
import io.jooby.Throwing;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.AsyncContext;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import static io.jooby.Throwing.throwingConsumer;
import static org.eclipse.jetty.server.Request.__MULTIPART_CONFIG_ELEMENT;

public class JettyContext extends BaseContext {
  private final Request request;
  private final Response response;
  private final Route.RootErrorHandler errorHandler;
  private final Path tmpdir;
  private final Executor worker;
  private QueryString query;
  private Formdata form;
  private Multipart multipart;
  private List<Upload> files;
  private Value.Object headers;

  public JettyContext(Request request, Executor worker, Route.RootErrorHandler errorHandler,
      Path tmpdir) {
    this.request = request;
    this.response = request.getResponse();
    this.errorHandler = errorHandler;
    this.tmpdir = tmpdir;

    // Worker:
    Connector connector = request.getHttpChannel().getConnector();
    this.worker = worker;
  }

  @Override public String name() {
    return "jetty";
  }

  @Nonnull @Override public Body body() {
    try {
      return Body.of(request.getInputStream(), request.getContentLengthLong());
    } catch (IOException x) {
      throw Throwing.sneakyThrow(x);
    }
  }

  @Nonnull @Override public String method() {
    return request.getMethod().toUpperCase();
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

  @Nonnull @Override public Formdata form() {
    if (form == null) {
      form = new Formdata();
      formParam(request, form);
    }
    return form;
  }

  @Nonnull @Override public Multipart multipart() {
    if (multipart == null) {
      multipart = new Multipart();
      form = multipart;

      request.setAttribute(__MULTIPART_CONFIG_ELEMENT,
          new MultipartConfigElement(tmpdir.toString(), -1L, -1L, Server._16KB));

      formParam(request, multipart);

      // Files:
      try {
        Collection<Part> parts = request.getParts();
        for (Part part : parts) {
          if (part.getSubmittedFileName() != null) {
            String name = part.getName();
            multipart.put(name,
                register(new JettyUpload(name, (MultiPartFormInputStream.MultiPart) part)));
          }
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

  @Nonnull @Override public Context dispatch(@Nonnull Runnable action) {
    return dispatch(worker, action);
  }

  @Nonnull @Override
  public Context dispatch(@Nonnull Executor executor, @Nonnull Runnable action) {
    if (worker == executor) {
      action.run();
    } else {
      if (!request.isAsyncStarted()) {
        request.startAsync();
      }
      executor.execute(action);
    }
    return this;
  }

  @Nonnull @Override public Context detach(@Nonnull Runnable action) {
    if (!request.isAsyncStarted()) {
      request.startAsync();
    }
    action.run();
    return this;
  }

  @Nonnull @Override public Map<String, Object> locals() {
    return locals;
  }

  @Nonnull @Override public Context statusCode(int statusCode) {
    response.setStatus(statusCode);
    return this;
  }

  @Nonnull @Override public Context type(@Nonnull String contentType, @Nullable String charset) {
    response.setContentType(contentType);
    if (charset != null)
      response.setCharacterEncoding(charset);
    return this;
  }

  @Nonnull @Override public Context header(@Nonnull String name, @Nonnull String value) {
    response.setHeader(name, value);
    return this;
  }

  @Nonnull @Override public Context length(long length) {
    response.setContentLengthLong(length);
    return this;
  }

  @Nonnull @Override public Context sendStatusCode(int statusCode) {
    try {
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
    return sendBytes(ByteBuffer.wrap(data));
  }

  @Nonnull @Override public Context sendText(@Nonnull String data, @Nonnull Charset charset) {
    return sendBytes(ByteBuffer.wrap(data.getBytes(charset)));
  }

  @Nonnull @Override public Context sendBytes(@Nonnull ByteBuffer data) {
    HttpOutput sender = response.getHttpOutput();
    if (request.isAsyncStarted()) {
      AsyncContext asyncContext = request.getAsyncContext();
      sender.sendContent(data, new JettyCallback(asyncContext));
    } else {
      sender.sendContent(data, Callback.NOOP);
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
      files.forEach(throwingConsumer(Upload::destroy));
    }
  }

  private static Handler gzipCall(Context ctx, Route.Handler next,
      AtomicReference<Object> result) {
    return new AbstractHandler() {
      @Override
      public void handle(String target, Request baseRequest, HttpServletRequest request,
          HttpServletResponse response) {
        try {
          result.set(next.apply(ctx));
        } catch (Throwable x) {
          result.set(x);
        }
      }
    };
  }

  private Upload register(Upload upload) {
    if (files == null) {
      files = new ArrayList<>();
    }
    files.add(upload);
    return upload;
  }

  private static void formParam(Request request, Formdata form) {
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
