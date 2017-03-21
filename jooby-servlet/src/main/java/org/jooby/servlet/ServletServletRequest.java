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
package org.jooby.servlet;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.jooby.Cookie;
import org.jooby.MediaType;
import org.jooby.spi.NativeRequest;
import org.jooby.spi.NativeUpload;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

public class ServletServletRequest implements NativeRequest {

  private HttpServletRequest req;

  private String tmpdir;

  private boolean multipart;

  private String path;

  private ServletUpgrade upgrade = noupgrade();

  public ServletServletRequest(final HttpServletRequest req, final String tmpdir,
      final boolean multipart) throws IOException {
    this.req = requireNonNull(req, "HTTP req is required.");
    this.tmpdir = requireNonNull(tmpdir, "A tmpdir is required.");
    this.multipart = multipart;
    String pathInfo = req.getPathInfo();
    if (pathInfo == null) {
      pathInfo = "/";
    }
    this.path = req.getContextPath() + URLDecoder.decode(pathInfo, "UTF-8");
  }

  public HttpServletRequest servletRequest() {
    return req;
  }

  public ServletServletRequest(final HttpServletRequest req, final String tmpdir)
      throws IOException {
    this(req, tmpdir, multipart(req));
  }

  public ServletServletRequest with(final ServletUpgrade upgrade) {
    this.upgrade = requireNonNull(upgrade, "An upgrade provider is required.");
    return this;
  }

  @Override
  public String method() {
    return req.getMethod();
  }

  @Override
  public Optional<String> queryString() {
    return Optional.ofNullable(Strings.emptyToNull(req.getQueryString()));
  }

  @Override
  public String path() {
    return path;
  }

  @Override
  public String rawPath() {
    return req.getRequestURI();
  }

  @Override
  public List<String> paramNames() {
    return toList(req.getParameterNames());
  }

  private <T> List<T> toList(final Enumeration<T> enumeration) {
    Builder<T> result = ImmutableList.builder();
    while (enumeration.hasMoreElements()) {
      result.add(enumeration.nextElement());
    }
    return result.build();
  }

  @Override
  public List<String> params(final String name) throws Exception {
    String[] values = req.getParameterValues(name);
    if (values == null) {
      return Collections.emptyList();
    }
    return Arrays.asList(values);
  }

  @Override
  public Map<String, Object> attributes() {
    final Enumeration<String> attributeNames = req.getAttributeNames();
    if (!attributeNames.hasMoreElements()) {
      return Collections.emptyMap();
    }
    return Collections.list(attributeNames).stream()
        .collect(Collectors.toMap(Function.identity(), name -> req.getAttribute(name)));
  }

  @Override
  public List<String> headers(final String name) {
    return toList(req.getHeaders(name));
  }

  @Override
  public Optional<String> header(final String name) {
    return Optional.ofNullable(req.getHeader(name));
  }

  @Override
  public List<String> headerNames() {
    return toList(req.getHeaderNames());
  }

  @Override
  public List<Cookie> cookies() {
    javax.servlet.http.Cookie[] cookies = req.getCookies();
    if (cookies == null) {
      return ImmutableList.of();
    }
    return Arrays.stream(cookies)
        .map(c -> {
          Cookie.Definition cookie = new Cookie.Definition(c.getName(), c.getValue());
          Optional.ofNullable(c.getComment()).ifPresent(cookie::comment);
          Optional.ofNullable(c.getDomain()).ifPresent(cookie::domain);
          Optional.ofNullable(c.getPath()).ifPresent(cookie::path);

          return cookie.toCookie();
        })
        .collect(Collectors.toList());
  }

  @Override
  public List<NativeUpload> files(final String name) throws IOException {
    try {
      if (multipart) {
        return req.getParts().stream()
            .filter(part -> part.getSubmittedFileName() != null && part.getName().equals(name))
            .map(part -> new ServletUpload(part, tmpdir))
            .collect(Collectors.toList());
      }
      return Collections.emptyList();
    } catch (ServletException ex) {
      throw new IOException("File not found: " + name, ex);
    }
  }

  @Override
  public InputStream in() throws IOException {
    return req.getInputStream();
  }

  @Override
  public String ip() {
    return req.getRemoteAddr();
  }

  @Override
  public String protocol() {
    return req.getProtocol();
  }

  @Override
  public boolean secure() {
    return req.isSecure();
  }

  @Override
  public <T> T upgrade(final Class<T> type) throws Exception {
    return upgrade.upgrade(type);
  }

  @Override
  public void startAsync(final Executor executor, final Runnable runnable) {
    req.startAsync();
    executor.execute(runnable);
  }

  private static boolean multipart(final HttpServletRequest req) {
    String contentType = req.getContentType();
    return contentType != null && contentType.toLowerCase().startsWith(MediaType.multipart.name());
  }

  private static ServletUpgrade noupgrade() {
    return new ServletUpgrade() {

      @Override
      public <T> T upgrade(final Class<T> type) throws Exception {
        throw new UnsupportedOperationException("");
      }
    };
  }
}
