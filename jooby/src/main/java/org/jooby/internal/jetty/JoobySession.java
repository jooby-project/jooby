package org.jooby.internal.jetty;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.server.session.MemSession;
import org.jooby.Cookie;
import org.jooby.Session;
import org.jooby.Session.Store.SaveReason;

import com.google.common.collect.ImmutableMap;

public class JoobySession extends MemSession implements Session {

  private boolean dirty;

  private long lastSave;

  private int saveInterval;

  private String secret;

  public JoobySession(final JoobySessionManager session, final HttpServletRequest request) {
    super(session, request);
  }

  @Override
  public String id() {
    return getId();
  }

  @Override
  public long createdAt() {
    return this.getCreationTime();
  }

  @Override
  public long accessedAt() {
    return this.getAccessed();
  }

  @Override
  public long expiryAt() {
    int maxInactiveInterval = getMaxInactiveInterval();
    if (maxInactiveInterval <= 0) {
      return 0;
    }
    return accessedAt() + TimeUnit.SECONDS.toMillis(maxInactiveInterval);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T get(final String name, final T defaults) {
    requireNonNull(name, "Attribute name is required.");
    Object attr = getAttribute(name);
    if (attr == null) {
      return defaults;
    }
    return (T) attr;
  }

  @Override
  public <T> T get(final String name) {
    return get(name, null);
  }

  @Override
  public Map<String, Object> attributes() {
    return ImmutableMap.copyOf(getAttributeMap());
  }

  @Override
  public Session set(final String name, final Object value) {
    requireNonNull(name, "Attribute name is required.");
    requireNonNull(value, "Attribute value is required.");
    setAttribute(name, value);
    return this;
  }

  @Override
  public Session unset(final String name) {
    requireNonNull(name, "Attribute name is required.");
    removeAttribute(name);
    return this;
  }

  @Override
  public Session unset() {
    clearAttributes();
    return this;
  }

  @Override
  public void destroy() {
    invalidate();
  }

  @Override
  public boolean access(final long time) {
    return super.access(time);
  }

  @Override
  public boolean isValid() {
    boolean valid = super.isValid();
    if (valid) {
      try {
        String sessionId = getClusterId();
        if (!sessionId.equals(Cookie.Signature.unsign(sessionId, secret))) {
          Session.log.warn("cookie signature invalid: {}", sessionId);
          return false;
        }
      } catch (Exception ex) {
        Session.log.warn("cookie signature invalid: " + getClusterId(), ex);
        return false;
      }
    }
    return valid;
  }

  @Override
  public void setClusterId(final String clusterId) {
    super.setClusterId(clusterId);
  }

  @Override
  public void setNodeId(final String nodeId) {
    super.setNodeId(nodeId);
  }

  public void setSaveInterval(final int saveInterval) {
    this.saveInterval = saveInterval;
  }

  public void setSecret(final String secret) {
    this.secret = secret;
  }

  @Override
  public JoobySessionManager getSessionManager() {
    return (JoobySessionManager) super.getSessionManager();
  }

  @Override
  public void setAttribute(final String name, final Object value) {
    Object old = changeAttribute(name, value);
    if (value == null && old == null) {
      return;
    }

    dirty = true;
  }

  @Override
  public void removeAttribute(final String name) {
    Object old = changeAttribute(name, null);
    if (old != null) {
      dirty = true;
    }
  }

  @Override
  protected void complete() {
    synchronized (this) {
      super.complete();
      try {
        if (isValid()) {
          if (dirty || isNew()) {
            getSessionManager().getSessionStore().save(this, SaveReason.DIRTY);
          } else {
            long access = getAccessed() - lastSave;
            long interval = saveInterval * 1000L;
            if (access >= interval) {
              getSessionManager().getSessionStore().save(this, SaveReason.TIME);
            }
          }
        }
      } catch (Exception ex) {
        log.warn("Can't save session: " + getId(), ex);
      } finally {
        dirty = false;
      }
    }
  }

  void setLastSave(final long lastSave) {
    this.lastSave = lastSave;
  }

  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder("  id: ").append(id()).append("\n");
    buffer.append("  createdAt: ").append(createdAt()).append("\n");
    buffer.append("  accessedAt: ").append(accessedAt()).append("\n");
    buffer.append("  expiryAt: ").append(expiryAt()).append("\n");
    buffer.append("  expiryAt: ").append(expiryAt());
    return buffer.toString();
  }

}
