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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jooby.Body;
import org.jooby.Cookie;
import org.jooby.Err;
import org.jooby.MediaType;
import org.jooby.Mutant;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.Status;
import org.jooby.Verb;
import org.jooby.fn.ExSupplier;
import org.jooby.internal.reqparam.RootParamConverter;
import org.jooby.spi.NativeResponse;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Injector;

public class ResponseImpl implements Response {

  private final Injector injector;

  private final NativeResponse rsp;

  private final Map<Object, Object> locals;

  private Route route;

  private Charset charset;

  private final Optional<String> referer;

  private BodyConverterSelector selector;

  private SetHeaderImpl setHeader;

  private Status status;

  private Map<String, Cookie> cookies = new HashMap<>();

  private List<String> clearCookies = new ArrayList<>();

  public ResponseImpl(final Injector injector,
      final NativeResponse rsp,
      final Route route,
      final Map<Object, Object> locals,
      final Charset charset,
      final Optional<String> referer) {
    this.injector = requireNonNull(injector, "An injector is required.");
    this.rsp = requireNonNull(rsp, "A raw response is required.");
    this.route = requireNonNull(route, "A route is required.");
    this.locals = requireNonNull(locals, "Request locals are required.");

    this.selector = injector.getInstance(BodyConverterSelector.class);
    this.setHeader = new SetHeaderImpl((name, value) -> {
      if (!committed()) {
        rsp.header(name, value);
      }
    });
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
    cookies.remove(name);
    clearCookies.add(name);
    return this;
  }

  @Override
  public Mutant header(final String name) {
    requireNonNull(name, "A header's name is required.");
    return new MutantImpl(injector.getInstance(RootParamConverter.class), rsp.headers(name));
  }

  @Override
  public Response header(final String name, final char value) {
    setHeader.header(name, value);
    return this;
  }

  @Override
  public Response header(final String name, final byte value) {
    setHeader.header(name, value);
    return this;
  }

  @Override
  public Response header(final String name, final short value) {
    setHeader.header(name, value);
    return this;
  }

  @Override
  public Response header(final String name, final int value) {
    setHeader.header(name, value);
    return this;
  }

  @Override
  public Response header(final String name, final long value) {
    setHeader.header(name, value);
    return this;
  }

  @Override
  public Response header(final String name, final float value) {
    setHeader.header(name, value);
    return this;
  }

  @Override
  public Response header(final String name, final double value) {
    setHeader.header(name, value);
    return this;
  }

  @Override
  public Response header(final String name, final CharSequence value) {
    setHeader.header(name, value);
    return this;
  }

  @Override
  public Response header(final String name, final Date value) {
    setHeader.header(name, value);
    return this;
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
    setHeader.header("Content-Length", length);
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
  public void send(final Body body) throws Exception {
    requireNonNull(body, "A body is required.");
    Optional<Object> content = body.content();
    Body.Formatter converter = content.isPresent()
        ? selector.formatter(content.get(), route.produces())
            .orElseThrow(() -> new Err(Status.NOT_ACCEPTABLE))
        : noop(route.produces());
    send(body, converter);
  }

  @Override
  public Formatter format() {
    final Map<MediaType, ExSupplier<Object>> strategies = new LinkedHashMap<>();
    List<MediaType> types = new LinkedList<>();

    return new Formatter() {

      @Override
      public Formatter when(final MediaType type, final ExSupplier<Object> supplier) {
        requireNonNull(type, "A media type is required.");
        requireNonNull(supplier, "A supplier fn is required.");
        strategies.put(type, supplier);
        types.add(type);
        return this;
      }

      @Override
      public void send() throws Exception {
        List<MediaType> produces = route.produces();

        ExSupplier<Object> provider = MediaType
            .matcher(produces)
            .first(types)
            .map(it -> strategies.get(it))
            .orElseThrow(
                () -> new Err(Status.NOT_ACCEPTABLE, Joiner.on(", ").join(produces))
            );

        Object result = provider.get();
        if (result instanceof Exception) {
          throw (Exception) result;
        }
        ResponseImpl.this.send(result);
      }

    };
  }

  @Override
  public void redirect(final Status status, final String location) throws Exception {
    requireNonNull(status, "A status is required.");
    requireNonNull(location, "A location is required.");

    send(Body.body(status).header("Location", location));
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
  public void end() throws IOException {
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
      final Body.Formatter formatter) throws Exception {

    contentDisposition(filename);

    Body body = Body.body(in);
    body.type(type().orElseGet(() -> MediaType.byPath(filename).orElse(MediaType.octetstream)));

    send(body, formatter);
  }

  public void send(final Body body, final Body.Formatter formatter) throws Exception {
    requireNonNull(body, "A response message is required.");
    requireNonNull(formatter, "A converter is required.");

    type(body.type().orElseGet(() -> type().orElseGet(() -> formatter.types().get(0))));

    status(body.status().orElseGet(() -> status().orElseGet(() -> Status.OK)));

    body.headers().forEach((name, value) -> {
      // reset back header while doing a redirect
        if ("location".equalsIgnoreCase(name) && "back".equalsIgnoreCase(value)
            && status().map(s -> s.value() >= 300 && s.value() < 400).orElse(false)) {
          header(name, referer.orElse("/"));
        } else {
          header(name, value);
        }
      });

    writeCookies();

    if (route.verb().is(Verb.HEAD)) {
      end();
      return;
    }

    /**
     * Do we need to figure it out Content-Length?
     */
    boolean direct = rsp.header("Content-Length").isPresent()
        || rsp.header("Transfer-Encoding").isPresent();

    // byte version of http body
    ExSupplier<OutputStream> stream = () -> {
      return direct
          ? rsp.out()
          : new FastByteArrayOutputStream(() -> rsp.out(), (name, value) -> header(name, value));
    };

    // text version of http body
    ExSupplier<Writer> writer = () -> {
      charset(charset());
      return new OutputStreamWriter(stream.get(), charset());
    };

    Optional<Object> content = body.content();
    if (content.isPresent()) {
      Object message = content.get();
      if (message instanceof Status) {
        // override status when message is a status
        status((Status) message);
      }

      formatter.format(message, new BodyWriterImpl(charset(), ImmutableMap.copyOf(locals),
          stream, writer));
    }
    // end response
    end();
  }

  private void writeCookies() {
    this.cookies.forEach((name, cookie) -> rsp.cookie(cookie));
    this.clearCookies.forEach(rsp::clearCookie);
    this.cookies.clear();
    this.clearCookies.clear();
  }

  public List<MediaType> viewableTypes() {
    return selector.viewableTypes();
  }

  public void reset() {
    status = null;
    this.cookies.clear();
    this.clearCookies.clear();
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

  private static Body.Formatter noop(final List<MediaType> types) {
    return new Body.Formatter() {

      @Override
      public void format(final Object body, final Body.Writer writer) throws Exception {
        writer.bytes(out -> out.close());
      }

      @Override
      public List<MediaType> types() {
        return types;
      }

      @Override
      public boolean canFormat(final Class<?> type) {
        return true;
      }

    };
  }

}
