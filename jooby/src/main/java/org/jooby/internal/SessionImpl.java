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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jooby.Mutant;
import org.jooby.Session;
import org.jooby.internal.reqparam.ParserExecutor;

import com.google.common.collect.ImmutableList;

public class SessionImpl implements Session {

  static class Builder implements Session.Builder {

    private SessionImpl session;

    public Builder(final ParserExecutor resolver, final boolean isNew, final String sessionId,
        final long timeout) {
      this.session = new SessionImpl(resolver, isNew, sessionId, timeout);
    }

    @Override
    public String sessionId() {
      return session.sessionId;
    }

    @Override
    public org.jooby.Session.Builder set(final String name, final String value) {
      session.attributes.put(name, value);
      return this;
    }

    @Override
    public Session.Builder set(final Map<String, String> attributes) {
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

  private ConcurrentMap<String, String> attributes = new ConcurrentHashMap<>();

  private String sessionId;

  private long createdAt;

  private volatile long accessedAt;

  private volatile long timeout;

  private volatile boolean isNew;

  private volatile boolean dirty;

  private volatile long savedAt;

  private ParserExecutor resolver;

  public SessionImpl(final ParserExecutor resolver, final boolean isNew, final String sessionId,
      final long timeout) {
    this.resolver = resolver;
    this.isNew = isNew;
    this.sessionId = sessionId;
    long now = System.currentTimeMillis();
    this.createdAt = now;
    this.accessedAt = now;
    this.savedAt = -1;
    this.timeout = timeout;
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

  @Override
  public Mutant get(final String name) {
    String value = attributes.get(name);
    List<String> values = value == null ? Collections.emptyList() : ImmutableList.of(value);
    return new MutantImpl(resolver, new StrParamReferenceImpl(name, values));
  }

  @Override
  public boolean isSet(final String name) {
    return attributes.containsKey(name);
  }

  @Override
  public Map<String, String> attributes() {
    return Collections.unmodifiableMap(attributes);
  }

  @Override
  public Session set(final String name, final String value) {
    requireNonNull(name, "An attribute name is required.");
    requireNonNull(value, "An attribute value is required.");
    String existing = attributes.put(name, value);
    dirty = existing == null || !existing.equals(value);
    return this;
  }

  @Override
  public Mutant unset(final String name) {
    String value = attributes.remove(name);
    List<String> values = Collections.emptyList();
    if (value != null) {
      values = ImmutableList.of(value);
      dirty = true;
    }
    return new MutantImpl(resolver, new StrParamReferenceImpl(name, values));
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
  }

  public void touch() {
    this.accessedAt = System.currentTimeMillis();
  }

  @Override
  public String toString() {
    return sessionId;
  }

  public void aboutToSave() {
    savedAt = System.currentTimeMillis();
  }
}
