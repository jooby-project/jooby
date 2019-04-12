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
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpHeaderValue;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.http.MultiPartFormInputStream;
import org.eclipse.jetty.server.HttpOutput;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.MultiMap;
import io.jooby.Throwing;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.Part;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.Executor;

import static org.eclipse.jetty.http.HttpHeader.CONTENT_TYPE;
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
  private Value headers;
  private Map<String, String> pathMap = Collections.EMPTY_MAP;
  private Map<String, Object> attributes = new HashMap<>();
  private Router router;
  private Route route;
  private MediaType responseType;

  public JettyContext(Request request, Router router, int bufferSize, long maxRequestSize) {
    this.request = request;
    this.response = request.getResponse();
    this.router = router;
    this.bufferSize = bufferSize;
    this.maxRequestSize = maxRequestSize;
  }

  @Nonnull @Override public Map<String, Object> getAttributes() {
    return attributes;
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

  @Nonnull @Override public Router getRouter() {
    return router;
  }

  @Nonnull @Override public String getMethod() {
    return request.getMethod().toUpperCase();
  }

  @Nonnull @Override public Route getRoute() {
    return route;
  }

  @Nonnull @Override public Context setRoute(Route route) {
    this.route = route;
    return this;
  }

  @Nonnull @Override public String pathString() {
    return request.getRequestURI();
  }

  @Nonnull @Override public Map<String, String> pathMap() {
    return pathMap;
  }

  @Nonnull @Override public Context setPathMap(Map<String, String> pathMap) {
    this.pathMap = pathMap;
    return this;
  }

  @Nonnull @Override public QueryString query() {
    if (query == null) {
      query = QueryString.create(request.getQueryString());
    }
    return query;
  }

  @Nonnull @Override public Formdata form() {
    if (form == null) {
      form = Formdata.create();
      formParam(request, form);
    }
    return form;
  }

  @Nonnull @Override public Multipart multipart() {
    if (multipart == null) {
      multipart = Multipart.create();
      form = multipart;

      request.setAttribute(__MULTIPART_CONFIG_ELEMENT,
          new MultipartConfigElement(router.getTmpdir().toString(), -1L, maxRequestSize,
              bufferSize));

      formParam(request, multipart);

      // Files:
      String contentType = request.getContentType();
      if (contentType != null &&
          MimeTypes.Type.MULTIPART_FORM_DATA.is(
              HttpFields.valueParameters(contentType, null))) {
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
    }
    return multipart;
  }

  @Nonnull @Override public Value headers() {
    if (headers == null) {
      Enumeration<String> names = request.getHeaderNames();
      Map<String, Collection<String>> headerMap = new LinkedHashMap<>();
      while (names.hasMoreElements()) {
        String name = names.nextElement();
        headerMap.put(name, Collections.list(request.getHeaders(name)));
      }
      headers = Value.hash(headerMap);
    }
    return headers;
  }

  @Nonnull @Override public String getRemoteAddress() {
    return request.getRemoteAddr();
  }

  @Nonnull @Override public String getProtocol() {
    return request.getProtocol();
  }

  @Nonnull @Override public String getScheme() {
    return request.getScheme();
  }

  @Override public boolean isInIoThread() {
    return false;
  }

  @Nonnull @Override public Context dispatch(@Nonnull Runnable action) {
    return dispatch(router.getWorker(), action);
  }

  @Nonnull @Override
  public Context dispatch(@Nonnull Executor executor, @Nonnull Runnable action) {
    if (router.getWorker() == executor) {
      action.run();
    } else {
      ifStartAsync();
      executor.execute(action);
    }
    return this;
  }

  @Nonnull @Override public Context detach(@Nonnull Runnable action) {
    ifStartAsync();
    action.run();
    return this;
  }

  @Nonnull @Override public StatusCode getStatusCode() {
    return StatusCode.valueOf(response.getStatus());
  }

  @Nonnull @Override public Context setStatusCode(int statusCode) {
    response.setStatus(statusCode);
    return this;
  }

  @Nonnull @Override public MediaType getResponseContentType() {
    return responseType == null ? MediaType.text : responseType;
  }

  @Nonnull @Override public Context setDefaultContentType(@Nonnull MediaType contentType) {
    if (responseType == null) {
      setContentType(contentType, contentType.getCharset());
    }
    return this;
  }

  @Nonnull @Override
  public Context setContentType(@Nonnull MediaType contentType, @Nullable Charset charset) {
    this.responseType = contentType;
    response.setHeader(CONTENT_TYPE, contentType.toContentTypeHeader(charset));
    return this;
  }

  @Nonnull @Override public Context setContentType(@Nonnull String contentType) {
    this.responseType = MediaType.valueOf(contentType);
    response.setHeader(CONTENT_TYPE, contentType);
    return this;
  }

  @Nonnull @Override public Context setHeader(@Nonnull String name, @Nonnull String value) {
    response.setHeader(name, value);
    return this;
  }

  @Nonnull @Override public Context setContentLength(long length) {
    response.setContentLengthLong(length);
    return this;
  }

  @Nonnull @Override public Sender responseSender() {
    ifSetChunked();
    ifStartAsync();
    return new JettySender(this, response.getHttpOutput());
  }

  @Nonnull @Override public OutputStream responseStream() {
    try {
      ifSetChunked();
      OutputStream outputStream = response.getOutputStream();
      return outputStream;
    } catch (IOException x) {
      throw Throwing.sneakyThrow(x);
    }
  }

  @Nonnull @Override public PrintWriter responseWriter(MediaType type, Charset charset) {
    try {
      ifSetChunked();
      setContentType(type, charset);
      return response.getWriter();
    } catch (IOException x) {
      throw Throwing.sneakyThrow(x);
    }
  }

  @Nonnull @Override public Context sendStatusCode(int statusCode) {
    response.setHeader(HttpHeader.TRANSFER_ENCODING, null);
    response.setLongContentLength(0);
    response.setStatus(statusCode);
    sendBytes(ByteBuffer.wrap(new byte[0]));
    return this;
  }

  @Nonnull @Override public Context sendBytes(@Nonnull byte[] data) {
    return sendBytes(ByteBuffer.wrap(data));
  }

  @Nonnull @Override public Context sendString(@Nonnull String data, @Nonnull Charset charset) {
    return sendBytes(ByteBuffer.wrap(data.getBytes(charset)));
  }

  @Nonnull @Override public Context sendBytes(@Nonnull ByteBuffer data) {
    HttpOutput sender = response.getHttpOutput();
    sender.sendContent(data, this);
    return this;
  }

  @Nonnull @Override public Context sendBytes(@Nonnull ReadableByteChannel channel) {
    ifSetChunked();
    ifStartAsync();
    HttpOutput sender = response.getHttpOutput();
    sender.sendContent(channel, this);
    return this;
  }

  @Nonnull @Override public Context sendStream(@Nonnull InputStream in) {
    try {
      if (in instanceof FileInputStream) {
        response.setLongContentLength(((FileInputStream) in).getChannel().size());
      }
      return sendStreamInternal(in);
    } catch (IOException x) {
      throw Throwing.sneakyThrow(x);
    }
  }

  private Context sendStreamInternal(@Nonnull InputStream in) {
    try {
      ifStartAsync();

      long len = response.getContentLength();
      InputStream stream;
      if (len > 0) {
        ByteRange range = ByteRange.parse(request.getHeader(HttpHeader.RANGE.asString()), len)
            .apply(this);
        stream = new ByteRangeInputStream(in, range);
      } else {
        response.setHeader(HttpHeader.TRANSFER_ENCODING, HttpHeaderValue.CHUNKED.asString());
        stream = in;
      }
      response.getHttpOutput().sendContent(stream, this);
      return this;
    } catch (IOException x) {
      throw Throwing.sneakyThrow(x);
    }
  }

  @Nonnull @Override public Context sendFile(@Nonnull FileChannel file) {
    try (FileChannel channel = file) {
      response.setLongContentLength(channel.size());
      return sendStreamInternal(Channels.newInputStream(file));
    } catch (IOException x) {
      throw Throwing.sneakyThrow(x);
    }
  }

  @Override public boolean isResponseStarted() {
    return response.isCommitted();
  }

  @Override public void succeeded() {
    destroy(null);
  }

  @Override public void failed(Throwable x) {
    destroy(x);
  }

  @Override public InvocationType getInvocationType() {
    return InvocationType.NON_BLOCKING;
  }

  void destroy(Throwable x) {
    Logger log = router.getLog();
    if (x != null) {
      if (Server.connectionLost(x)) {
        log.debug("exception found while sending response {} {}", getMethod(), pathString(), x);
      } else {
        log.error("exception found while sending response {} {}", getMethod(), pathString(), x);
      }
    }
    if (files != null) {
      for (FileUpload file : files) {
        try {
          file.destroy();
        } catch (Exception e) {
          log.debug("file upload destroy resulted in exception", e);
        }
      }
      files.clear();
      files = null;
    }
    try {
      if (request.isAsyncStarted()) {
        request.getAsyncContext().complete();
      } else {
        response.closeOutput();
      }
    } catch (IOException e) {
      log.debug("exception found while closing resources {} {} {}", getMethod(), pathString(), e);
    }
    this.router = null;
    this.request = null;
    this.response = null;
  }

  private void ifStartAsync() {
    if (!request.isAsyncStarted()) {
      request.startAsync();
    }
  }

  private void ifSetChunked() {
    if (response.getContentLength() <= 0) {
      response.setHeader(HttpHeader.TRANSFER_ENCODING, HttpHeaderValue.CHUNKED.asString());
    }
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
