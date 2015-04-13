/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.internal;

import static java.util.Objects.requireNonNull;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

import org.jooby.BodyFormatter;
import org.jooby.Cookie;
import org.jooby.Err;
import org.jooby.MediaType;
import org.jooby.Mutant;
import org.jooby.Response;
import org.jooby.Result;
import org.jooby.Results;
import org.jooby.Route;
import org.jooby.Status;
import org.jooby.internal.reqparam.ParamResolver;
import org.jooby.spi.NativeResponse;
import org.jooby.util.Collectors;
import org.jooby.util.ExSupplier;

import com.google.common.base.Joiner;
import com.google.inject.Injector;

public class ResponseImpl implements Response {

  private final Injector injector;

  private final NativeResponse rsp;

  private final Map<String, Object> locals;

  private Route route;

  private Charset charset;

  private final Optional<String> referer;

  private BodyConverterSelector selector;

  private Status status;

  private Map<String, Cookie> cookies = new HashMap<>();

  private int maxBufferSize;

  public ResponseImpl(final Injector injector,
      final NativeResponse rsp, final int maxBufferSize, final Route route,
      final Map<String, Object> locals, final Charset charset, final Optional<String> referer) {
    this.injector = requireNonNull(injector, "An injector is required.");
    this.rsp = requireNonNull(rsp, "A raw response is required.");
    this.maxBufferSize = maxBufferSize;
    this.route = requireNonNull(route, "A route is required.");
    this.locals = requireNonNull(locals, "Request locals are required.");

    this.selector = injector.getInstance(BodyConverterSelector.class);
    this.charset = requireNonNull(charset, "A charset is required.");
    this.referer = requireNonNull(referer, "A referer header is required.");
  }

  @Override
  public void download(final String filename, final InputStream stream) throws Exception {
    requireNonNull(filename, "A file's name is required.");
    requireNonNull(stream, "A stream is required.");

    download(filename, stream, BuiltinBodyConverter.formatStream);
  }

  @Override
  public void download(final String filename, final Reader reader) throws Exception {
    requireNonNull(filename, "A file's name is required.");
    requireNonNull(reader, "A reader is required.");

    download(filename, reader, BuiltinBodyConverter.formatReader);
  }

  @Override
  public Response cookie(final Cookie cookie) {
    requireNonNull(cookie, "A cookie is required.");
    cookies.put(cookie.name(), cookie);
    return this;
  }

  @Override
  public Response clearCookie(final String name) {
    requireNonNull(name, "A cookie's name is required.");
    if (cookies.remove(name) == null) {
      // cookie was set in a previous req, we must send a expire header.
      cookies.put(name, new Cookie.Definition(name, "").maxAge(0).toCookie());
    }
    return this;
  }

  @Override
  public Mutant header(final String name) {
    requireNonNull(name, "A header's name is required.");
    return new MutantImpl(injector.getInstance(ParamResolver.class), rsp.headers(name));
  }

  @Override
  public Response header(final String name, final Object value) {
    requireNonNull(name, "Header's name is required.");
    requireNonNull(value, "Header's value is required.");

    return setHeader(name, value);
  }

  @Override
  public Response header(final String name, final Iterable<Object> values) {
    requireNonNull(name, "Header's name is required.");
    requireNonNull(values, "Header's values are required.");

    return setHeader(name, values);
  }

  @Override
  public Charset charset() {
    return charset;
  }

  @Override
  public Response charset(final Charset charset) {
    this.charset = requireNonNull(charset, "A charset is required.");
    type(type().orElse(MediaType.html));
    return this;
  }

  @Override
  public Response length(final long length) {
    rsp.header("Content-Length", Headers.encode(length));
    return this;
  }

  @Override
  public Optional<MediaType> type() {
    return rsp.header("Content-Type").map(MediaType::valueOf);
  }

  @Override
  public Response type(final MediaType type) {
    if (type.isText()) {
      header("Content-Type", type.name() + ";charset=" + charset.name());
    } else {
      header("Content-Type", type.name());
    }
    return this;
  }

  @Override
  public void redirect(final Status status, final String location) throws Exception {
    requireNonNull(status, "A status is required.");
    requireNonNull(location, "A location is required.");

    send(Results.with(status).header("Location", location));
  }

  @Override
  public Optional<Status> status() {
    return Optional.ofNullable(status);
  }

  @Override
  public Response status(final Status status) {
    this.status = requireNonNull(status, "A status is required.");
    rsp.statusCode(status.value());
    return this;
  }

  @Override
  public boolean committed() {
    return rsp.committed();
  }

  @Override
  public void end() {
    if (!committed()) {
      if (status == null) {
        status(rsp.statusCode());
      }

      writeCookies();

      /**
       * Do we need to figure it out Content-Length?
       */
      boolean lenSet = rsp.header("Content-Length").isPresent()
          || rsp.header("Transfer-Encoding").isPresent();
      if (!lenSet) {
        int statusCode = status.value();
        boolean hasBody = true;
        if (statusCode >= 100 && statusCode < 200) {
          hasBody = false;
        } else if (statusCode == 204 || statusCode == 304) {
          hasBody = false;
        }
        if (hasBody) {
          rsp.header("Content-Length", "0");
        }
      }
    }
    rsp.end();
  }

  private void download(final String filename, final Object in,
      final BodyFormatter formatter) throws Exception {

    contentDisposition(filename);

    Result result = Results.with(in);
    result.type(type().orElseGet(() -> MediaType.byPath(filename).orElse(MediaType.octetstream)));

    send(result, formatter);
  }

  @Override
  public void send(final Result result) throws Exception {
    requireNonNull(result, "A result is required.");
    List<MediaType> produces = route.produces();
    Optional<Object> entity = result.get(produces);
    BodyFormatter converter = entity.isPresent()
        ? selector.formatter(entity.get(), produces)
            .orElseThrow(() -> new Err(Status.NOT_ACCEPTABLE, Joiner.on(", ").join(produces)))
        : null;
    send(result, entity, converter);
  }

  public void send(final Result result, final BodyFormatter formatter) throws Exception {
    requireNonNull(result, "A response message is required.");
    requireNonNull(formatter, "A converter is required.");

    List<MediaType> produces = route.produces();
    Optional<Object> entity = result.get(produces);
    send(result, entity, formatter);
  }

  private void send(final Result result, final Optional<Object> entity,
      final BodyFormatter fmt) throws Exception {

    type(result.type().orElseGet(() -> type()
        .orElseGet(() -> fmt == null ? MediaType.html : fmt.types().get(0))));

    status(result.status().orElseGet(() -> status().orElseGet(() -> Status.OK)));

    writeCookies();

    result.headers().forEach((name, value) -> {
      header(name, value);
    });

    if (route.method().equals("HEAD")) {
      end();
      return;
    }

    /**
     * Do we need to figure it out Content-Length?
     */
    long len = rsp.header("Content-Length").map(Long::parseLong).orElse((long) Integer.MAX_VALUE);
    int bufferSize = Math.min(maxBufferSize, (int) len);

    // byte version of http body
    ExSupplier<OutputStream> stream = () -> {
      return rsp.out(bufferSize);
    };

    // text version of http body
    ExSupplier<Writer> writer = () -> {
      charset(charset());
      return new OutputStreamWriter(stream.get(), charset());
    };

    if (entity.isPresent()) {
      Object message = entity.get();
      if (message instanceof Status) {
        // override status when message is a status
        status((Status) message);
      }

      fmt.format(message, new BodyFormatterContext(charset(), Collections.unmodifiableMap(locals),
          stream, writer));
    }
    // end response
    end();
  }

  private void writeCookies() {
    if (cookies.size() > 0) {
      rsp.header("Set-Cookie",
          cookies.values().stream().map(Cookie::encode).collect(Collectors.toList()));
    }
    this.cookies.clear();
  }

  public void reset() {
    status = null;
    this.cookies.clear();
    rsp.reset();
  }

  void route(final Route route) {
    this.route = route;
  }

  private void contentDisposition(final String filename) {
    String basename = filename;
    int last = filename.lastIndexOf('/');
    if (last >= 0) {
      basename = basename.substring(last + 1);
    }
    header("Content-Disposition", "attachment; filename=" + basename);
  }

  @SuppressWarnings("unchecked")
  private Response setHeader(final String name, final Object value) {
    if (!committed()) {
      if (value instanceof Iterable) {
        List<String> values = StreamSupport.stream(((Iterable<Object>) value).spliterator(), false)
            .map(Headers::encode)
            .collect(Collectors.toList());
        rsp.header(name, values);
      } else {
        if ("location".equalsIgnoreCase(name) && "back".equalsIgnoreCase(value.toString())) {
          rsp.header(name, referer.orElse("/"));
        } else {
          rsp.header(name, Headers.encode(value));
        }
      }
    }

    return this;
  }

}
