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
import io.jooby.Cookie;
import io.jooby.SessionId;

public class SecretSessionId implements SessionId {

  private SessionId sessionId;

  private final String secret;

  public SecretSessionId(SessionId sessionId, String secret) {
    this.sessionId = sessionId;
    this.secret = secret;
  }

  @Override public String findSessionId(Context ctx) {
    String sessionId = this.sessionId.findSessionId(ctx);
    return sessionId == null ? null : Cookie.unsign(sessionId, secret);
  }

  @Override public void saveSessionId(Context ctx, String sessionId) {
    this.sessionId.saveSessionId(ctx, Cookie.sign(sessionId, secret));
  }

  @Override public void deleteSessionId(Context ctx, String sessionId) {
    this.sessionId.deleteSessionId(ctx, sessionId);
  }
}
