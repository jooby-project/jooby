/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.pac4j;

import io.jooby.Context;
import io.jooby.Value;
import io.jooby.pac4j.Pac4jContext;
import org.pac4j.core.context.Cookie;
import org.pac4j.core.context.session.SessionStore;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WebContextImpl implements Pac4jContext {

  private Context context;

  public WebContextImpl(Context context) {
    this.context = context;
  }

  @Override
  public Context getContext() {
    return context;
  }

  @Override public SessionStore getSessionStore() {
    return new SessionStoreImpl();
  }

  @Override public Optional<String> getRequestParameter(String name) {
    return Stream.of(context.path(), context.query(), context.multipart())
        .map(v -> v.get(name))
        .filter(v -> !v.isMissing())
        .findFirst()
        .map(Value::value);
  }

  @Override public Map<String, String[]> getRequestParameters() {
    Map<String, String[]> all = new LinkedHashMap<>();
    parameters(context.path().toMultimap(), all::put);
    parameters(context.query().toMultimap(), all::put);
    parameters(context.multipart().toMultimap(), all::put);
    return all;
  }

  private void parameters(Map<String, List<String>> params, BiConsumer<String, String[]> consumer) {
    params.forEach((k, v) -> consumer.accept(k, v.toArray(new String[v.size()])));
  }

  @Override public Optional getRequestAttribute(String name) {
    Object value = context.getAttributes().get(name);
    return Optional.ofNullable(value);
  }

  @Override public void setRequestAttribute(String name, Object value) {
    context.attribute(name, value);
  }

  @Override public Optional<String> getRequestHeader(String name) {
    return context.header(name).toOptional();
  }

  @Override public String getRequestMethod() {
    return context.getMethod();
  }

  @Override public String getRemoteAddr() {
    return context.getRemoteAddress();
  }

  @Override public void setResponseHeader(String name, String value) {
    context.setResponseHeader(name, value);
  }

  @Override public void setResponseContentType(String content) {
    context.setResponseType(content);
  }

  @Override public String getServerName() {
    return context.getServerHost();
  }

  @Override public int getServerPort() {
    return context.getServerPort();
  }

  @Override public String getScheme() {
    return context.getScheme();
  }

  @Override public boolean isSecure() {
    return context.isSecure();
  }

  @Override public String getFullRequestURL() {
    return context.getRequestURL();
  }

  @Override public Collection<Cookie> getRequestCookies() {
    return context.cookieMap().entrySet().stream()
        .map(e -> new Cookie(e.getKey(), e.getValue()))
        .collect(Collectors.toList());
  }

  @Override public void addResponseCookie(Cookie cookie) {
    io.jooby.Cookie rsp = new io.jooby.Cookie(cookie.getName(), cookie.getValue());
    Optional.ofNullable(cookie.getDomain()).ifPresent(rsp::setDomain);
    Optional.ofNullable(cookie.getPath()).ifPresent(rsp::setPath);
    rsp.setHttpOnly(cookie.isHttpOnly());
    rsp.setMaxAge(cookie.getMaxAge());
    rsp.setSecure(cookie.isSecure());

    context.setResponseCookie(rsp);
  }

  @Override public String getPath() {
    return context.pathString();
  }
}
