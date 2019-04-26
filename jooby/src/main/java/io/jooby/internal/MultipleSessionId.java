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
