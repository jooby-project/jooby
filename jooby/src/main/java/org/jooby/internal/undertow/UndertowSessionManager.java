package org.jooby.internal.undertow;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.Session;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionListener;
import io.undertow.server.session.SessionManager;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.jooby.Cookie;
import org.jooby.Session.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.google.common.util.concurrent.UncheckedExecutionException;

public class UndertowSessionManager implements SessionManager {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  static final ThreadLocal<HttpServerExchange> exchange = new ThreadLocal<HttpServerExchange>();

  private Cache<String, UndertowSession> sessions;

  private Store store;

  private String secret;

  private int saveInterval;

  public UndertowSessionManager(final org.jooby.Session.Store store,
      final int timeout, final int saveInterval, final String secret) {
    this.store = store;
    this.secret = secret;
    this.saveInterval = saveInterval;
    sessions = CacheBuilder.newBuilder()
        .expireAfterAccess(timeout, TimeUnit.SECONDS)
        .removalListener(onEvict(store, log))
        .build();
  }

  @Override
  public String getDeploymentName() {
    return store.getClass().getSimpleName();
  }

  @Override
  public void start() {
  }

  @Override
  public void stop() {
  }

  @Override
  public UndertowSession createSession(final HttpServerExchange exchange,
      final SessionConfig config) {
    String sessionId = generateID(exchange, config);

    try {
      return sessions.get(sessionId, () -> {
        config.setSessionId(exchange, sign(sessionId));
        return new UndertowSession(this, sessionId, saveInterval);
      });
    } catch (ExecutionException | UncheckedExecutionException ex) {
      Throwables.propagateIfPossible(ex.getCause());
      throw new IllegalStateException("Unable to create session: " + sessionId, ex);
    }
  }

  @Override
  public UndertowSession getSession(final HttpServerExchange exchange, final SessionConfig config) {
    return safeSession(config.findSessionId(exchange), sessionId -> {
      try {
        return sessions.get(sessionId, () -> {
          UndertowSession session = (UndertowSession) store.get(
              new UndertowSession.Builder(this, sessionId, saveInterval));
          return session;
        });
      } catch (CacheLoader.InvalidCacheLoadException ignored) {
        return null;
      } catch (ExecutionException | UncheckedExecutionException ex) {
        Throwables.propagateIfPossible(ex.getCause());
        throw new IllegalStateException("Unable to create session: " + sessionId, ex);
      }
    });
  }

  public UndertowSession trySession(final HttpServerExchange exchange, final SessionConfig config) {
    return safeSession(config.findSessionId(exchange), sessionId ->
        sessions.getIfPresent(sessionId));
  }

  public void save(final UndertowSession session) {
    store.save(session, null);
  }

  public void invalidate(final UndertowSession session) {
    String sessionId = session.getId();
    // remove from cookie
    Optional.ofNullable(exchange.get()).ifPresent(exchange -> session.invalidate(exchange));

    // remove from cache
    sessions.invalidate(sessionId);

    if (!session.isNew()) {
      Optional.ofNullable(exchange.get())
          .ifPresent(exchange -> exchange.dispatch(() -> store.delete(sessionId)));
    }
  }

  @Override
  public UndertowSession getSession(final String sessionId) {
    return safeSession(sessionId,
        safeSessionId -> sessions.getIfPresent(safeSessionId));
  }

  @Override
  public void registerSessionListener(final SessionListener listener) {
  }

  @Override
  public void removeSessionListener(final SessionListener listener) {
  }

  @Override
  public void setDefaultSessionTimeout(final int timeout) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<String> getTransientSessions() {
    return getAllSessions();
  }

  @Override
  public Set<String> getActiveSessions() {
    return getAllSessions();
  }

  @Override
  public Set<String> getAllSessions() {
    return sessions.asMap().keySet();
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof UndertowSessionManager) {
      return getDeploymentName().equals(((UndertowSessionManager) obj).getDeploymentName());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return getDeploymentName().hashCode();
  }

  private static RemovalListener<String, UndertowSession> onEvict(
      final org.jooby.Session.Store store, final Logger log) {
    return notification -> {
      RemovalCause removalCause = notification.getCause();
      String sessionId = notification.getKey();
      Session session = notification.getValue();
      log.info("removing: {} reason: {}", sessionId, removalCause);
      if (exchange.get() != null && removalCause == RemovalCause.EXPIRED) {
        session.invalidate(exchange.get());
        exchange.get().getConnection().getWorker().execute(() ->
          store.delete(sessionId)
        );
      }
    };
  }

  private UndertowSession safeSession(final String sessionId,
      final Function<String, UndertowSession> sessionGet) {
    if (sessionId == null) {
      return null;
    }
    if (secret == null) {
      return sessionGet.apply(sessionId);
    }
    return Optional.ofNullable(Cookie.Signature.unsign(sessionId, secret)).map(sessionGet)
        .orElse(null);
  }

  String sign(final String sessionId) {
    return secret == null ? sessionId : Cookie.Signature.sign(sessionId, secret);
  }

  private String generateID(final long seed) {
    String sessionId = store.generateID(seed);
    if (sessionId == null) {
      UUID uuid = UUID.randomUUID();
      sessionId = Long.toString(Math.abs(uuid.getMostSignificantBits()), 36)
          + Long.toString(Math.abs(uuid.getLeastSignificantBits()), 36);
    }
    return sessionId;
  }

  private String generateID(final HttpServerExchange exchange, final SessionConfig config) {
    String sessionId = config.findSessionId(exchange);
    long seed = System.identityHashCode(exchange);
    if (sessionId == null) {
      sessionId = generateID(seed);
    }
    return sessionId;
  }

}
