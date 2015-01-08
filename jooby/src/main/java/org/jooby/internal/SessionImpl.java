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
/**
 * This copy of Woodstox XML processor is licensed under the
 * Apache (Software) License, version 2.0 ("the License").
 * See the License for details about distribution rights, and the
 * specific rights regarding derivate works.
 *
 * You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/
 *
 * A copy is also included in the downloadable source code package
 * containing Woodstox, in file "ASL2.0", under the same directory
 * as this file.
 */
package org.jooby.internal;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.jooby.Session;

import com.google.common.collect.ImmutableMap;

public class SessionImpl implements Session {

  static class Builder implements Session.Builder {

    private SessionImpl session;

    public Builder(final boolean isNew, final String sessionId, final long timeout) {
      this.session = new SessionImpl(isNew, sessionId, timeout);
    }

    @Override
    public String sessionId() {
      return session.sessionId;
    }

    @Override
    public org.jooby.Session.Builder set(final String name, final Object value) {
      session.attributes.put(name, value);
      return this;
    }

    @Override
    public Session.Builder set(final Map<String, Object> attributes) {
      session.attributes.putAll(attributes);
      return this;
    }

    @Override
    public Session.Builder createdAt(final long createdAt) {
      session.createdAt = createdAt;
      return this;
    }

    @Override
    public Session.Builder accessedAt(final long accessedAt) {
      session.accessedAt = accessedAt;
      return this;
    }

    @Override
    public Session.Builder savedAt(final long savedAt) {
      session.savedAt = savedAt;
      return this;
    }

    @Override
    public Session build() {
      requireNonNull(session.sessionId, "Session's id wasn't set.");
      return session;
    }

  }

  private ConcurrentMap<String, Object> attributes = new ConcurrentHashMap<>();

  private String sessionId;

  private long createdAt;

  private volatile long accessedAt;

  private volatile long timeout;

  private volatile boolean isNew;

  private volatile boolean dirty;

  private volatile long savedAt;

  public SessionImpl(final boolean isNew, final String sessionId, final long timeout) {
    this.isNew = isNew;
    this.sessionId = sessionId;
    long now = System.currentTimeMillis();
    this.createdAt = now;
    this.accessedAt = now;
    this.savedAt = -1;
    this.timeout = TimeUnit.SECONDS.toMillis(timeout);
  }

  @Override
  public String id() {
    return sessionId;
  }

  @Override
  public long createdAt() {
    return createdAt;
  }

  @Override
  public long accessedAt() {
    return accessedAt;
  }

  @Override
  public long expiryAt() {
    if (timeout <= 0) {
      return -1;
    }
    return accessedAt + timeout;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> Optional<T> get(final String name) {
    T value = (T) attributes.get(name);
    return Optional.ofNullable(value);
  }

  @Override
  public Map<String, Object> attributes() {
    return ImmutableMap.copyOf(attributes);
  }

  @Override
  public Session set(final String name, final Object value) {
    requireNonNull(name, "An attribute name is required.");
    requireNonNull(value, "An attribute value is required.");
    Object existing = attributes.put(name, value);
    dirty = existing == null || !existing.equals(value);
    return this;
  }

  @Override
  public <T> Optional<T> unset(final String name) {
    @SuppressWarnings("unchecked")
    T value = (T) attributes.remove(name);
    if (value != null) {
      dirty = true;
    }
    return Optional.ofNullable(value);
  }

  @Override
  public Session unset() {
    attributes.clear();
    dirty = true;
    return this;
  }

  @Override
  public void destroy() {
    unset();
  }

  public boolean isNew() {
    return isNew;
  }

  public boolean isDirty() {
    return dirty;
  }

  @Override
  public long savedAt() {
    return savedAt;
  }

  void markAsSaved() {
    isNew = false;
    dirty = false;
    savedAt = System.currentTimeMillis();
  }

  public void touch() {
    this.accessedAt = System.currentTimeMillis();
  }

  public boolean validate() {
    if (timeout <= 0) {
      return true;
    }
    long now = System.currentTimeMillis();
    return now - accessedAt < timeout;
  }

  @Override
  public String toString() {
    return sessionId;
  }
}
