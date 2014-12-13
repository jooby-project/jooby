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
package org.jooby.internal.jetty;

import static java.util.Objects.requireNonNull;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Optional;
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

  public JoobySession(final JoobySessionManager manager, final HttpServletRequest request) {
    super(manager, request);
  }

  public JoobySession(final JoobySessionManager manager, final long createdAt,
      final long accessedAt, final String clusterId) {
    super(manager, createdAt, accessedAt, clusterId);
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
      return -1;
    }
    return accessedAt() + TimeUnit.SECONDS.toMillis(maxInactiveInterval);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Optional<T> get(final String name) {
    requireNonNull(name, "Attribute name is required.");
    T attr = (T) getAttribute(name);
    return Optional.ofNullable(attr);
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
  public <T> Optional<T> unset(final String name) {
    requireNonNull(name, "Attribute name is required.");
    @SuppressWarnings("unchecked")
    T value = (T) changeAttribute(name, null);
    if (value != null) {
      dirty = true;
      return Optional.of(value);
    } else {
      return Optional.empty();
    }
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
      if (secret != null) {
        try {
          String sessionId = getClusterId();
          if (!Cookie.Signature.valid(sessionId, secret)) {
            Session.log.warn("cookie signature invalid: {}", sessionId);
            return false;
          }
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
          Session.log.warn("cookie signature invalid: " + getClusterId(), ex);
          return false;
        }
      }
    }
    return valid;
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
        log.error("Can't save session: " + getId(), ex);
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
