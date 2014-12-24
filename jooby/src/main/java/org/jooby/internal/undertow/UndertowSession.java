package org.jooby.internal.undertow;

import static java.util.Objects.requireNonNull;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionManager;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.jooby.Session;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

public class UndertowSession implements Session, io.undertow.server.session.Session {

  public static class Builder implements Session.Builder {

    private UndertowSession session;

    public Builder(final UndertowSessionManager manager, final String sessionId,
        final int saveInterval) {
      this.session = new UndertowSession(manager, sessionId, saveInterval);
    }

    @Override
    public String sessionId() {
      return session.getId();
    }

    @Override
    public org.jooby.Session.Builder set(final String name, final Object value) {
      session.attributes.put(name, value);
      return this;
    }

    @Override
    public org.jooby.Session.Builder set(final Map<String, Object> attributes) {
      session.attributes.putAll(attributes);
      return this;
    }

    @Override
    public org.jooby.Session.Builder createdAt(final long createdAt) {
      session.createdAt = createdAt;
      return this;
    }

    @Override
    public org.jooby.Session.Builder accessedAt(final long accessedAt) {
      session.lastAccessed = accessedAt;
      return this;
    }

    @Override
    public Session build() {
      return session;
    }

  }

  private UndertowSessionManager manager;

  private String sessionId;

  private long createdAt;

  private volatile long lastAccessed;

  private volatile boolean dirty;

  private volatile boolean isNew;

  private volatile long lastSaved;

  private int maxInactiveInterval;

  private Map<String, Object> attributes;

  private final long saveInterval;

  public UndertowSession(final UndertowSessionManager manager, final String sessionId,
      final int saveInterval) {
    this.manager = manager;
    this.sessionId = sessionId;
    this.createdAt = System.currentTimeMillis();
    this.lastAccessed = this.createdAt;
    this.saveInterval = TimeUnit.SECONDS.toMillis(saveInterval);
    this.lastSaved = this.createdAt;
    this.dirty = true;
    this.isNew = true;
    attributes = new ConcurrentHashMap<>();
  }

  @Override
  public String getId() {
    return sessionId;
  }

  @Override
  public synchronized void requestDone(final HttpServerExchange exchange) {
    long now = System.currentTimeMillis();
    boolean saveInvervalExpired = (now - lastSaved >= saveInterval);
    this.lastAccessed = now;
    if (dirty || saveInvervalExpired) {
      dirty = false;
      isNew = false;
      lastSaved = now;
      exchange.dispatch(() -> manager.save(this));
    }
  }

  @Override
  public long getCreationTime() {
    return createdAt;
  }

  @Override
  public long getLastAccessedTime() {
    return lastAccessed;
  }

  @Override
  public void setMaxInactiveInterval(final int interval) {
    this.maxInactiveInterval = interval;
  }

  @Override
  public int getMaxInactiveInterval() {
    return maxInactiveInterval;
  }

  @Override
  public Object getAttribute(final String name) {
    return attributes.get(name);
  }

  @Override
  public Set<String> getAttributeNames() {
    return attributes.keySet();
  }

  @Override
  public Object setAttribute(final String name, final Object value) {
    dirty = true;
    return attributes.put(name, value);
  }

  @Override
  public Object removeAttribute(final String name) {
    Object value = attributes.remove(name);
    dirty = value != null;
    return value;
  }

  @Override
  public void invalidate(final HttpServerExchange exchange) {
    attributes.clear();

    if (exchange != null) {
      SessionConfig sessionConfig = exchange.getAttachment(SessionConfig.ATTACHMENT_KEY);
      String cookieId = manager.sign(sessionId);
      LoggerFactory.getLogger(getClass()).info("clearing cookieId {}", cookieId);
      sessionConfig.clearSession(exchange, cookieId);
    }
  }

  public boolean isNew() {
    return isNew;
  }

  @Override
  public SessionManager getSessionManager() {
    return manager;
  }

  @Override
  public String changeSessionId(final HttpServerExchange exchange, final SessionConfig config) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String id() {
    return getId();
  }

  @Override
  public long createdAt() {
    return createdAt;
  }

  @Override
  public long accessedAt() {
    return lastAccessed;
  }

  @Override
  public long expiryAt() {
    int maxInactiveInterval = getMaxInactiveInterval();
    if (maxInactiveInterval <= 0) {
      return -1;
    }
    return lastAccessed + TimeUnit.SECONDS.toMillis(maxInactiveInterval);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> Optional<T> get(final String name) {
    requireNonNull(name, "Attribute name is required.");
    T attr = (T) getAttribute(name);
    return Optional.ofNullable(attr);
  }

  @Override
  public Map<String, Object> attributes() {
    return ImmutableMap.copyOf(attributes);
  }

  @Override
  public Session set(final String name, final Object value) {
    requireNonNull(name, "Attribute name is required.");
    requireNonNull(value, "Attribute value is required.");
    setAttribute(name, value);
    return this;
  }

  @Override
  public <T> Optional<T> unset(final String name) {
    requireNonNull(name, "Attribute name is required.");
    @SuppressWarnings("unchecked")
    T value = (T) attributes.remove(name);
    if (value != null) {
      dirty = true;
      return Optional.of(value);
    } else {
      return Optional.empty();
    }
  }

  @Override
  public Session unset() {
    dirty = true;
    attributes.clear();
    return this;
  }

  @Override
  public void destroy() {
    manager.invalidate(this);
  }

}
