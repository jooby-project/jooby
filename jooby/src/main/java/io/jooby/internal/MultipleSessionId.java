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
import io.jooby.SessionId;

import java.util.ArrayList;
import java.util.List;

public class MultipleSessionId implements SessionId {

  private SessionId[] sessionIds;

  public MultipleSessionId(SessionId... sessionIds) {
    this.sessionIds = sessionIds;
  }

  @Override public String findSessionId(Context ctx) {
    for (SessionId sessionId : sessionIds) {
      String sid = sessionId.findSessionId(ctx);
      if (sid != null) {
        return sid;
      }
    }
    return null;
  }

  @Override public void saveSessionId(Context ctx, String sessionId) {
    strategy(ctx).forEach(it -> it.saveSessionId(ctx, sessionId));
  }

  @Override public void deleteSessionId(Context ctx, String sessionId) {
    strategy(ctx).forEach(it -> it.deleteSessionId(ctx, sessionId));
  }

  private List<SessionId> strategy(Context ctx) {
    List<SessionId> result = new ArrayList<>(sessionIds.length);
    for (SessionId strategy : sessionIds) {
      if (strategy.findSessionId(ctx) != null) {
        result.add(strategy);
      }
    }
    return result;
  }
}
