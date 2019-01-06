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
import org.eclipse.jetty.server.HttpOutput;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.MultiMap;
import io.jooby.Throwing;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.Executor;

import static io.jooby.Throwing.throwingConsumer;
import static org.eclipse.jetty.server.Request.__MULTIPART_CONFIG_ELEMENT;

public class JettyContext implements Callback, Context {
  private final int bufferSize;
  private final long maxRequestSize;
  private Request request;
  private Response response;
  private QueryString query;
  private Formdata form;
  private Multipart multipart;
  private List<FileUpload> files;
  private Value.Object headers;
  private Map<String, String> pathMap = Collections.EMPTY_MAP;
  private Map<String, Object> locals = Collections.EMPTY_MAP;
  private Router router;
  private Route route;

  public JettyContext(Request request, Router router, int bufferSize, long maxRequestSize) {
    this.request = request;
    this.response = request.getResponse();
    this.router = router;
    this.bufferSize = bufferSize;
    this.maxRequestSize = maxRequestSize;
  }

  @Nonnull @Override public Map<String, Object> locals() {
    return locals;
  }

  @Nullable @Override public <T> T get(String name) {
    return (T) locals.get(name);
  }

  @Nonnull @Override public Context set(@Nonnull String name, @Nonnull Object value) {
    if (locals == Collections.EMPTY_MAP) {
      locals = new HashMap<>();
    }
    locals.put(name, value);
    return this;
  }

  @Override public String name() {
    return "jetty";
  }

  @Nonnull @Override public Body body() {
    try {
      InputStream in = request.getInputStream();
      long len = request.getContentLengthLong();
      if (maxRequestSize > 0) {
        in = new LimitedInputStream(in, maxRequestSize);
      }
      return Body.of(in, len);
    } catch (IOException x) {
      throw Throwing.sneakyThrow(x);
    }
  }

  @Nonnull @Override public Router router() {
    return router;
  }

  @Nonnull @Override public String method() {
    return request.getMethod().toUpperCase();
  }

  @Nonnull @Override public Route route() {
    return route;
  }

  @Nonnull @Override public Context route(Route route) {
    this.route = route;
    return this;
  }

  @Nonnull @Override public String pathString() {
    return request.getRequestURI();
  }

  @Nonnull @Override public Map<String, String> pathMap() {
    return pathMap;
  }

  @Nonnull @Override public Context pathMap(Map<String, String> pathMap) {
    this.pathMap = pathMap;
    return this;
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
          new MultipartConfigElement(router.tmpdir().toString(), -1L, maxRequestSize, bufferSize));

      formParam(request, multipart);

      // Files:
      try {
        Collection<Part> parts = request.getParts();
        for (Part part : parts) {
          if (part.getSubmittedFileName() != null) {
            String name = part.getName();
            multipart.put(name,
                register(new JettyFileUpload(name, (MultiPartFormInputStream.MultiPart) part)));
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
    return dispatch(router.worker(), action);
  }

  @Nonnull @Override
  public Context dispatch(@Nonnull Executor executor, @Nonnull Runnable action) {
    if (router.worker() == executor) {
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

  @Nonnull @Override
  public Context responseChannel(Throwing.Consumer<WritableByteChannel> consumer) {
    HttpOutput output = response.getHttpOutput();
    try {
      consumer.accept(Channels.newChannel(output));
    } finally {
      if (output != null && !output.isClosed()) {
        output.close();
      }
    }
    return this;
  }

  @Nonnull @Override public Context outputStream(Throwing.Consumer<OutputStream> consumer) {
    HttpOutput output = response.getHttpOutput();
    try {
      consumer.accept(output);
    } finally {
      if (output != null && !output.isClosed()) {
        output.close();
      }
    }
    return this;
  }

  @Nonnull @Override public Context writer(Charset charset, Throwing.Consumer<Writer> consumer)
      throws Exception {
    response.setCharacterEncoding(charset.name());
    try (Writer writer = response.getWriter()) {
      consumer.accept(writer);
    }
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
    sender.sendContent(data, this);
    return this;
  }

  @Override public boolean isResponseStarted() {
    return request.getResponse().isCommitted();
  }

  @Override public void succeeded() {
    destroy();
  }

  @Override public void failed(Throwable x) {
    destroy();
  }

  @Override public InvocationType getInvocationType() {
    return InvocationType.NON_BLOCKING;
  }

  private void destroy() {
    if (files != null) {
      files.forEach(throwingConsumer(FileUpload::destroy));
      files.clear();
      files = null;
    }
    if (request.isAsyncStarted()) {
      request.getAsyncContext().complete();
    }
    this.router = null;
    this.request = null;
    this.response = null;
  }

  private FileUpload register(FileUpload upload) {
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
