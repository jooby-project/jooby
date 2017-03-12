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
package org.jooby.internal.pac4j;

import com.google.common.collect.ImmutableMap;
import org.jooby.Cookie.Definition;
import org.jooby.*;
import org.pac4j.core.context.Cookie;
import org.pac4j.core.context.WebContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class AuthContext implements WebContext {

  /**
   * The logging system.
   */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private Request req;

  private Response rsp;

  private Map<String, String[]> params;

  @Inject
  public AuthContext(final Request req, final Response rsp) {
    this.req = req;
    this.rsp = rsp;
    params = params(req);
  }

  @Override
  public String getRequestParameter(final String name) {
    String[] values = params.get(name);
    return values == null ? null : values[0];
  }

  @Override
  public Map<String, String[]> getRequestParameters() {
    return params;
  }

  @Override
  public String getRequestHeader(final String name) {
    return req.header(name).value(null);
  }

  @Override
  public void setSessionAttribute(final String name, final Object value) {
    Session session = req.session();
    if (value == null) {
      session.unset(name);
    } else {
      session.set(name, AuthSerializer.objToStr(value));
    }
  }

  @Override
  public Object getSessionAttribute(final String name) {
    Session session = req.session();
    return AuthSerializer.strToObject(session.get(name).value(null));
  }

  @Override
  public String getRequestMethod() {
    return req.method();
  }

  @Override
  public void writeResponseContent(final String content) {
    try {
      rsp.send(content);
    } catch (Throwable ex) {
      throw new Err(Status.SERVER_ERROR, ex);
    }
  }

  @Override
  public void setResponseStatus(final int code) {
    rsp.status(code);
  }

  @Override
  public void setResponseHeader(final String name, final String value) {
    rsp.header(name, value);
  }

  @Override
  public String getServerName() {
    return req.hostname();
  }

  @Override
  public int getServerPort() {
    return req.port();
  }

  @Override
  public String getScheme() {
    return req.secure() ? "https" : "http";
  }

  @Override
  public String getFullRequestURL() {
    String query = req.queryString().map(it -> "?" + it).orElse("");
    return getScheme() + "://" + getServerName() + ":" + getServerPort() + req.contextPath() + req
        .path() + query;
  }

  private Map<String, String[]> params(final Request req) {
    ImmutableMap.Builder<String, String[]> result = ImmutableMap.<String, String[]>builder();

    req.params().toMap().forEach((name, value) -> {
      try {
        List<String> values = value.toList();
        result.put(name, values.toArray(new String[values.size()]));
      } catch (Err ignored) {
        log.debug("ignoring HTTP param: " + name, ignored);
      }
    });
    return result.build();
  }

  @Override
  public String toString() {
    return req.toString();
  }

  @Override
  public Object getRequestAttribute(final String name) {
    Optional<Object> attr = req.ifGet(name);
    return attr.orElse(null);
  }

  @Override
  public void setRequestAttribute(final String name, final Object value) {
    req.set(name, value);
  }

  @Override
  public Object getSessionIdentifier() {
    return req.session().id();
  }

  @Override
  public String getRemoteAddr() {
    return req.ip();
  }

  @Override
  public void setResponseContentType(final String content) {
    rsp.type(content);
  }

  @Override
  public String getPath() {
    return req.path();
  }

  @Override
  public boolean isSecure() {
    return req.secure();
  }

  @Override
  public Collection<Cookie> getRequestCookies() {
    return req.cookies().stream().map(c -> {
      Cookie cookie = new Cookie(c.name(), c.value().orElse(null));
      c.domain().ifPresent(cookie::setDomain);
      c.path().ifPresent(cookie::setPath);
      cookie.setHttpOnly(c.httpOnly());
      cookie.setSecure(c.secure());
      return cookie;
    }).collect(Collectors.toList());
  }

  @Override
  public void addResponseCookie(final Cookie cookie) {
    Definition c = new Definition(cookie.getName(), cookie.getValue());
    Optional.ofNullable(cookie.getDomain()).ifPresent(c::domain);
    Optional.ofNullable(cookie.getPath()).ifPresent(c::path);
    c.httpOnly(cookie.isHttpOnly());
    c.maxAge(cookie.getMaxAge());
    c.secure(cookie.isSecure());
    rsp.cookie(c);
  }

}
