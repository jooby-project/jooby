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
import org.pac4j.core.exception.http.BadRequestAction;
import org.pac4j.core.exception.http.ForbiddenAction;
import org.pac4j.core.exception.http.FoundAction;
import org.pac4j.core.exception.http.HttpAction;
import org.pac4j.core.exception.http.NoContentAction;
import org.pac4j.core.exception.http.OkAction;
import org.pac4j.core.exception.http.SeeOtherAction;
import org.pac4j.core.exception.http.StatusAction;
import org.pac4j.core.exception.http.TemporaryRedirectAction;
import org.pac4j.core.exception.http.UnauthorizedAction;
import org.pac4j.core.exception.http.WithContentAction;
import org.pac4j.core.exception.http.WithLocationAction;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import java.util.Optional;

import static io.jooby.StatusCode.BAD_REQUEST_CODE;
import static io.jooby.StatusCode.FORBIDDEN_CODE;
import static io.jooby.StatusCode.FOUND_CODE;
import static io.jooby.StatusCode.NO_CONTENT_CODE;
import static io.jooby.StatusCode.OK_CODE;
import static io.jooby.StatusCode.SEE_OTHER_CODE;
import static io.jooby.StatusCode.TEMPORARY_REDIRECT_CODE;
import static io.jooby.StatusCode.UNAUTHORIZED_CODE;

public class SessionStoreImpl
    implements org.pac4j.core.context.session.SessionStore<Pac4jContext> {

  private static final String PAC4J = "p4j~";

  private static final String BIN = "b64~";

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
    if (value == null || value.toString().length() == 0) {
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
    //getSessionOrEmpty(context).ifPresent(session -> session.renewId());
    return true;
  }

  static Optional<Object> strToObject(final Value node) {
    if (node.isMissing()) {
      return Optional.empty();
    }
    String value = node.value();
    if (value.startsWith(BIN)) {
      try {
        byte[] bytes = Base64.getDecoder().decode(value.substring(BIN.length()));
        return Optional.of(new ObjectInputStream(new ByteArrayInputStream(bytes)).readObject());
      } catch (Exception x) {
        throw SneakyThrows.propagate(x);
      }
    } else if (value.startsWith(PAC4J)) {
      return Optional.of(strToAction(value.substring(PAC4J.length())));
    }
    return Optional.of(value);
  }

  static String objToStr(final Object value) {
    if (value instanceof CharSequence || value instanceof Number || value instanceof Boolean) {
      return value.toString();
    } else if (value instanceof HttpAction) {
      return actionToStr((HttpAction) value);
    }
    try {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      ObjectOutputStream stream = new ObjectOutputStream(bytes);
      stream.writeObject(value);
      stream.flush();
      return BIN + Base64.getEncoder().encodeToString(bytes.toByteArray());
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  private static String actionToStr(HttpAction action) {
    StringBuilder buffer = new StringBuilder();
    buffer.append(PAC4J).append(action.getCode());
    if (action instanceof WithContentAction) {
      buffer.append(":").append(((WithContentAction) action).getContent());
    } else if (action instanceof WithLocationAction) {
      buffer.append(":").append(((WithLocationAction) action).getLocation());
    }
    return buffer.toString();
  }

  private static HttpAction strToAction(String value) {
    int i = value.indexOf(":");
    int code;
    String tail;
    if (i > 0) {
      code = Integer.parseInt(value.substring(0, i));
      tail = value.substring(i + 1);
    } else {
      code = Integer.parseInt(value);
      tail = null;
    }
    switch (code) {
      case BAD_REQUEST_CODE:
        return BadRequestAction.INSTANCE;
      case FORBIDDEN_CODE:
        return ForbiddenAction.INSTANCE;
      case FOUND_CODE:
        return new FoundAction(tail);
      case NO_CONTENT_CODE:
        return NoContentAction.INSTANCE;
      case OK_CODE:
        return new OkAction(tail);
      case TEMPORARY_REDIRECT_CODE:
        return new TemporaryRedirectAction(tail);
      case SEE_OTHER_CODE:
        return new SeeOtherAction(tail);
      case UNAUTHORIZED_CODE:
        return UnauthorizedAction.INSTANCE;
      default:
        return new StatusAction(code);
    }
  }
}
