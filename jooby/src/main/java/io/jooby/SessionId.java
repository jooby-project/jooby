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
package io.jooby;

public interface SessionId {

  String findSessionId(Context ctx);

  void saveSessionId(Context ctx, String sessionId);

  void deleteSessionId(Context ctx, String sessionId);

  static SessionId cookie(Cookie cookie) {
    String name = cookie.getName();
    return new SessionId() {
      @Override public String findSessionId(Context ctx) {
        return ctx.cookieMap().get(name);
      }

      @Override public void saveSessionId(Context ctx, String sessionId) {
        String existingId = findSessionId(ctx);
        // write cookie for new or expiring session
        if (existingId == null || cookie.getMaxAge() > 0) {
          ctx.setResponseCookie(cookie.clone().setValue(sessionId));
        }
      }

      @Override public void deleteSessionId(Context ctx, String sessionId) {
        ctx.setResponseCookie(cookie.clone().setMaxAge(0));
      }
    };
  }

  static SessionId header(String name) {
    return new SessionId() {
      @Override public String findSessionId(Context ctx) {
        return ctx.headerMap().get(name);
      }

      @Override public void saveSessionId(Context ctx, String sessionId) {
        ctx.setResponseHeader(name, sessionId);
      }

      @Override public void deleteSessionId(Context ctx, String sessionId) {
        ctx.removeResponseHeader(name);
      }
    };
  }
}
