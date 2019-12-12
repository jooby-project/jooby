/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.pac4j;

import io.jooby.Session;
import io.jooby.SneakyThrows;
import io.jooby.Value;
import io.jooby.pac4j.Pac4jContext;
import org.pac4j.core.context.session.SessionStore;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import java.util.Optional;

public class SessionStoreImpl
    implements org.pac4j.core.context.session.SessionStore<Pac4jContext> {

  private static final String PREFIX = "b64~";

  private Session getSession(Pac4jContext context) {
    return context.getContext().session();
  }

  private Optional<Session> getSessionOrEmpty(Pac4jContext context) {
    return Optional.ofNullable(context.getContext().sessionOrNull());
  }

  @Override public String getOrCreateSessionId(Pac4jContext context) {
    return getSession(context).getId();
  }

  @Override public Optional<Object> get(Pac4jContext context, String key) {
    Optional sessionValue = getSessionOrEmpty(context)
        .map(session -> session.get(key))
        .map(SessionStoreImpl::strToObject)
        .orElseGet(Optional::empty);
    return sessionValue;
  }

  @Override public void set(Pac4jContext context, String key, Object value) {
    if (value == null) {
      getSessionOrEmpty(context).ifPresent(session -> session.remove(key));
    } else {
      String encoded = objToStr(value);
      getSession(context).put(key, encoded);
    }
  }

  @Override public boolean destroySession(Pac4jContext context) {
    Optional<Session> session = getSessionOrEmpty(context);
    session.ifPresent(Session::destroy);
    return session.isPresent();
  }

  @Override public Optional getTrackableSession(Pac4jContext context) {
    return getSessionOrEmpty(context);
  }

  @Override public Optional<SessionStore<Pac4jContext>> buildFromTrackableSession(
      Pac4jContext context, Object trackableSession) {
    if (trackableSession != null) {
      return Optional.of(new SessionStoreImpl());
    }
    return Optional.empty();
  }

  @Override public boolean renewSession(Pac4jContext context) {
    getSessionOrEmpty(context).ifPresent(session -> session.renewId());
    return true;
  }

  static final Optional<Object> strToObject(final Value node) {
    if (node.isMissing()) {
      return Optional.empty();
    }
    String value = node.value();
    if (!value.startsWith(PREFIX)) {
      return Optional.of(value);
    }
    try {
      byte[] bytes = Base64.getDecoder().decode(value.substring(PREFIX.length()));
      return Optional.of(new ObjectInputStream(new ByteArrayInputStream(bytes)).readObject());
    } catch (Exception x) {
      throw SneakyThrows.propagate(x);
    }
  }

  static final String objToStr(final Object value) {
    if (value instanceof CharSequence || value instanceof Number || value instanceof Boolean) {
      return value.toString();
    }
    try {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      ObjectOutputStream stream = new ObjectOutputStream(bytes);
      stream.writeObject(value);
      stream.flush();
      return PREFIX + Base64.getEncoder().encodeToString(bytes.toByteArray());
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }
}
