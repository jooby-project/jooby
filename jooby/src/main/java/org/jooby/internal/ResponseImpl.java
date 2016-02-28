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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.jooby.Asset;
import org.jooby.Cookie;
import org.jooby.Deferred;
import org.jooby.MediaType;
import org.jooby.Mutant;
import org.jooby.Renderer;
import org.jooby.Response;
import org.jooby.Result;
import org.jooby.Results;
import org.jooby.Route;
import org.jooby.Status;
import org.jooby.internal.parser.ParserExecutor;
import org.jooby.spi.NativeResponse;

import com.google.common.collect.ImmutableList;

public class ResponseImpl implements Response {

  /** Char encoded content disposition. */
  private static final String CONTENT_DISPOSITION = "attachment; filename=\"%s\"; filename*=%s''%s";

  private final NativeResponse rsp;

  private final Map<String, Object> locals;

  private Route route;

  private Charset charset;

  private final Optional<String> referer;

  private Status status;

  private MediaType type;

  private long len = -1;

  private Map<String, Cookie> cookies = new HashMap<>();

  private List<Renderer> renderers;

  private ParserExecutor parserExecutor;

  private Map<String, Renderer> rendererMap;

  public ResponseImpl(final ParserExecutor parserExecutor,
      final NativeResponse rsp, final Route route, final List<Renderer> renderers,
      final Map<String, Renderer> rendererMap, final Map<String, Object> locals,
      final Charset charset, final Optional<String> referer) {
    this.parserExecutor = parserExecutor;
    this.rsp = rsp;
    this.route = route;
    this.locals = locals;
    this.renderers = renderers;
    this.rendererMap = rendererMap;
    this.charset = charset;
    this.referer = referer;
  }

  @Override
  public void download(final String filename, final InputStream stream) throws Exception {
    requireNonNull(filename, "A file's name is required.");
    requireNonNull(stream, "A stream is required.");

    // handle type
    MediaType type = MediaType.byPath(filename).orElse(MediaType.octetstream);
    type(type().orElseGet(() -> type));

    Asset asset = new InputStreamAsset(stream, filename, type().get());
    contentDisposition(filename);
    send(Results.with(asset.stream()));
  }

  @Override
  public void download(final String filename, final String location) throws Exception {
    URL url = getClass().getResource(location.startsWith("/") ? location : "/" + location);
    if (url == null) {
      throw new FileNotFoundException(location);
    }
    // handle type
    MediaType type = MediaType.byPath(filename).orElse(MediaType.byPath(location)
        .orElse(MediaType.octetstream));
    type(type().orElseGet(() -> type));

    URLAsset asset = new URLAsset(url, location, type().get());
    length(asset.length());

    contentDisposition(filename);
    send(Results.with(asset));
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
    return new MutantImpl(parserExecutor, new StrParamReferenceImpl(name, rsp.headers(name)));
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
    len = length;
    rsp.header("Content-Length", Headers.encode(length));
    return this;
  }

  @Override
  public Optional<MediaType> type() {
    return Optional.ofNullable(type);
  }

  @Override
  public Response type(final MediaType type) {
    this.type = requireNonNull(type, "MediaType is required.");
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
    if (!rsp.committed()) {
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

  @Override
  public void send(final Result result) throws Exception {
    requireNonNull(result, "A result is required.");

    if (result instanceof Deferred) {
      throw new DeferredExecution((Deferred) result);
    }

    Optional<MediaType> rtype = result.type();
    if (rtype.isPresent()) {
      type(rtype.get());
    }

    List<MediaType> produces = this.type == null ? route.produces() : ImmutableList.of(type);

    status(result.status().orElseGet(() -> status().orElseGet(() -> Status.OK)));

    result.headers().forEach((name, value) -> {
      setHeader(name, value);
    });

    writeCookies();

    if (route.method().equals("HEAD")) {
      end();
      return;
    }

    /**
     * Do we need to figure it out Content-Length?
     */
    Optional<Object> value = result.get(produces);

    if (value.isPresent()) {
      Object message = value.get();
      if (message instanceof Status) {
        // override status when message is a status
        status((Status) message);
      }

      Consumer<Long> setLen = len -> {
        if (this.len == -1 && len >= 0) {
          length(len);
        }
      };

      Consumer<MediaType> setType = type -> {
        if (this.type == null) {
          type(type);
        }
      };

      HttpRendererContext ctx = new HttpRendererContext(
          renderers,
          rsp,
          setLen,
          setType,
          locals,
          produces,
          charset);
      // explicit renderer?
      Renderer renderer = rendererMap.get(route.attr("renderer"));
      if (renderer != null) {
        renderer.render(message, ctx);
      } else {
        ctx.render(message);
      }
    }
    // end response
    end();
  }

  private void writeCookies() {
    if (cookies.size() > 0) {
      rsp.header("Set-Cookie",
          cookies.values().stream().map(Cookie::encode).collect(Collectors.toList()));
      cookies.clear();
    }
  }

  public void reset() {
    status = null;
    this.cookies.clear();
    rsp.reset();
  }

  void route(final Route route) {
    this.route = route;
  }

  private void contentDisposition(final String filename) throws IOException {
    String basename = filename;
    int last = filename.lastIndexOf('/');
    if (last >= 0) {
      basename = basename.substring(last + 1);
    }

    String ebasename = URLEncoder.encode(basename, charset.name()).replaceAll("\\+", "%20");
    header("Content-Disposition",
        String.format(CONTENT_DISPOSITION, basename, charset.name(), ebasename));
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
