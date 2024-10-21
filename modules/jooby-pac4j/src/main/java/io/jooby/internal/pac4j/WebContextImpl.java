/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.pac4j;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.pac4j.core.context.Cookie;
import org.pac4j.core.context.session.SessionStore;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.SameSite;
import io.jooby.Value;
import io.jooby.pac4j.Pac4jContext;
import io.jooby.pac4j.Pac4jOptions;

public class WebContextImpl implements Pac4jContext {

  private Context context;
  private SessionStore sessionStore;

  public WebContextImpl(Context context) {
    this.context = context;
    this.sessionStore = new SessionStoreImpl();
  }

  @Override
  public @NonNull Context getContext() {
    return context;
  }

  @Override
  public Optional<String> getResponseHeader(String name) {
    return Optional.ofNullable(this.context.getResponseHeader(name));
  }

  @Override
  public Optional<String> getRequestParameter(String name) {
    return Stream.of(context.path(), context.query(), context.form())
        .map(v -> v.get(name))
        .filter(v -> !v.isMissing())
        .findFirst()
        .map(Value::value);
  }

  @Override
  public Map<String, String[]> getRequestParameters() {
    Map<String, String[]> all = new LinkedHashMap<>();
    parameters(context.path().toMultimap(), all::put);
    parameters(context.query().toMultimap(), all::put);
    parameters(context.form().toMultimap(), all::put);
    return all;
  }

  private void parameters(Map<String, List<String>> params, BiConsumer<String, String[]> consumer) {
    params.forEach((k, v) -> consumer.accept(k, v.toArray(new String[0])));
  }

  @Override
  public Optional getRequestAttribute(String name) {
    Object value = context.getAttributes().get(name);
    return Optional.ofNullable(value);
  }

  @Override
  public void setRequestAttribute(String name, Object value) {
    context.setAttribute(name, value);
  }

  @Override
  public Optional<String> getRequestHeader(String name) {
    return context.header(name).toOptional();
  }

  @Override
  public String getRequestMethod() {
    return context.getMethod();
  }

  @Override
  public String getRemoteAddr() {
    return context.getRemoteAddress();
  }

  @Override
  public void setResponseHeader(String name, String value) {
    context.setResponseHeader(name, value);
  }

  @Override
  public void setResponseContentType(String content) {
    context.setResponseType(content);
  }

  @Override
  public String getServerName() {
    return context.getServerHost();
  }

  @Override
  public int getServerPort() {
    return context.getServerPort();
  }

  @Override
  public String getScheme() {
    return context.getScheme();
  }

  @Override
  public boolean isSecure() {
    return context.isSecure();
  }

  @Override
  public String getFullRequestURL() {
    return context.getRequestURL();
  }

  @Override
  public Collection<Cookie> getRequestCookies() {
    return context.cookieMap().entrySet().stream()
        .map(e -> new Cookie(e.getKey(), e.getValue()))
        .collect(Collectors.toList());
  }

  @Override
  public void addResponseCookie(Cookie cookie) {
    io.jooby.Cookie rsp = new io.jooby.Cookie(cookie.getName(), cookie.getValue());
    Optional.ofNullable(cookie.getDomain()).ifPresent(rsp::setDomain);
    Optional.ofNullable(cookie.getPath()).ifPresent(rsp::setPath);
    rsp.setHttpOnly(cookie.isHttpOnly());
    rsp.setMaxAge(cookie.getMaxAge());
    rsp.setSecure(cookie.isSecure());

    SameSite sameSite = context.require(Pac4jOptions.class).getCookieSameSite();
    if (sameSite != null) {
      rsp.setSecure(rsp.isSecure() || sameSite.requiresSecure());
      rsp.setSameSite(sameSite);
    }

    context.setResponseCookie(rsp);
  }

  @Override
  public String getPath() {
    return context.getRequestPath();
  }

  @NonNull @Override
  public SessionStore getSessionStore() {
    return sessionStore;
  }
}
