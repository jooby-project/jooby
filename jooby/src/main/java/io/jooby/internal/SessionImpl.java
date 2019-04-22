/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.Session;
import io.jooby.Value;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionImpl implements Session {

  private boolean isNew;

  private String id;

  private ConcurrentHashMap<String, String> attributes = new ConcurrentHashMap<>();

  private Instant creationTime;

  private Instant lastAccessedTime;

  private boolean modify;

  public SessionImpl(String id) {
    this.id = id;
  }

  @Override public boolean isNew() {
    return isNew;
  }

  @Nonnull @Override public Session setNew(boolean aNew) {
    this.isNew = aNew;
    return this;
  }

  @Override public boolean isModify() {
    return modify;
  }

  @Nonnull @Override public Session setModify(boolean modify) {
    this.modify = modify;
    return this;
  }

  @Override public @Nonnull String getId() {
    return id;
  }

  @Override public @Nonnull Value get(@Nonnull String name) {
    String value = attributes.get(name);
    return value == null ? Value.missing(name) : Value.value(name, value);
  }

  @Override public @Nonnull Session put(@Nonnull String name, int value) {
    attributes.put(name, Integer.toString(value));
    updateFlags();
    return this;
  }

  @Override public @Nonnull Session put(@Nonnull String name, long value) {
    attributes.put(name, Long.toString(value));
    updateFlags();
    return this;
  }

  @Override public @Nonnull Session put(@Nonnull String name, CharSequence value) {
    attributes.put(name, value.toString());
    updateFlags();
    return this;
  }

  @Override public @Nonnull Session put(@Nonnull String name, String value) {
    attributes.put(name, value);
    updateFlags();
    return this;
  }

  @Override public @Nonnull Session put(@Nonnull String name, float value) {
    attributes.put(name, Float.toString(value));
    updateFlags();
    return this;
  }

  @Override public @Nonnull Session put(@Nonnull String name, double value) {
    attributes.put(name, Double.toString(value));
    updateFlags();
    return this;
  }

  @Override public @Nonnull Session put(@Nonnull String name, boolean value) {
    attributes.put(name, Boolean.toString(value));
    updateFlags();
    return this;
  }

  @Override public @Nonnull Session put(@Nonnull String name, Number value) {
    attributes.put(name, value.toString());
    updateFlags();
    return this;
  }

  @Override public @Nonnull Value remove(@Nonnull String name) {
    String value = attributes.remove(name);
    updateFlags();
    return value == null ? Value.missing(name) : Value.value(name, value);
  }

  @Override public @Nonnull Map<String, String> toMap() {
    return Collections.unmodifiableMap(attributes);
  }

  @Override public @Nonnull Instant getCreationTime() {
    return creationTime;
  }

  @Nonnull @Override public Session setCreationTime(Instant creationTime) {
    this.creationTime = creationTime;
    return this;
  }

  @Override public @Nonnull Instant getLastAccessedTime() {
    return lastAccessedTime;
  }

  @Override public @Nonnull Session setLastAccessedTime(@Nonnull Instant lastAccessedTime) {
    this.lastAccessedTime = lastAccessedTime;
    return this;
  }

  @Override public void destroy() {
    attributes.clear();
  }

  private void updateFlags() {
    modify = true;
    lastAccessedTime = Instant.now();
  }
}
