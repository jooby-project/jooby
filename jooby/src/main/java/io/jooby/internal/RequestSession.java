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

import io.jooby.Context;
import io.jooby.Session;
import io.jooby.SessionId;
import io.jooby.SessionOptions;
import io.jooby.Value;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.Map;

public class RequestSession implements Session {

  private Context context;

  private Session session;

  public RequestSession(Context context, Session session) {
    this.context = context;
    this.session = session;
    context.attribute("session", this);
  }

  @Nonnull @Override public String getId() {
    return session.getId();
  }

  @Nonnull @Override public Value get(@Nonnull String name) {
    return session.get(name);
  }

  @Nonnull @Override public Session put(@Nonnull String name, int value) {
    session.put(name, value);
    return this;
  }

  @Nonnull @Override public Session put(@Nonnull String name, long value) {
    session.put(name, value);
    return this;
  }

  @Nonnull @Override public Session put(@Nonnull String name, CharSequence value) {
    session.put(name, value);
    return this;
  }

  @Nonnull @Override public Session put(@Nonnull String name, String value) {
    session.put(name, value);
    return this;
  }

  @Nonnull @Override public Session put(@Nonnull String name, float value) {
    session.put(name, value);
    return this;
  }

  @Nonnull @Override public Session put(@Nonnull String name, double value) {
    session.put(name, value);
    return this;
  }

  @Nonnull @Override public Session put(@Nonnull String name, boolean value) {
    return null;
  }

  @Nonnull @Override public Session put(@Nonnull String name, Number value) {
    session.put(name, value);
    return this;
  }

  @Nonnull @Override public Value remove(@Nonnull String name) {
    return session.remove(name);
  }

  @Nonnull @Override public Map<String, String> toMap() {
    return session.toMap();
  }

  @Nonnull @Override public Instant getCreationTime() {
    return session.getCreationTime();
  }

  @Nonnull @Override public Session setCreationTime(@Nonnull Instant creationTime) {
    session.setCreationTime(creationTime);
    return this;
  }

  @Nonnull @Override public Instant getLastAccessedTime() {
    return session.getLastAccessedTime();
  }

  @Nonnull @Override public Session setLastAccessedTime(@Nonnull Instant lastAccessedTime) {
    session.setLastAccessedTime(lastAccessedTime);
    return this;
  }

  @Override public boolean isModify() {
    return session.isModify();
  }

  @Nonnull @Override public Session setModify(boolean modify) {
    session.setModify(modify);
    return this;
  }

  @Override public boolean isNew() {
    return session.isNew();
  }

  @Nonnull @Override public Session setNew(boolean isNew) {
    session.setNew(isNew);
    return this;
  }

  public Session getSession() {
    return session;
  }

  @Override public Session clear() {
    session.clear();
    return this;
  }

  public void destroy() {
    if (context != null) {
      try {
        context.getAttributes().remove("session");
        SessionOptions options = context.getRouter().getSessionOptions();
        String sessionId = session.getId();
        for (SessionId strategy : options.getSessionId()) {
          strategy.deleteSessionId(context, sessionId);
        }
        options.getStore().deleteSession(sessionId);
      } finally {
        session.destroy();
        context = null;
        session = null;
      }
    }
  }
}
