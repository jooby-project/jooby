/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.Context;
import io.jooby.SessionToken;

import java.util.ArrayList;
import java.util.List;

public class MultipleSessionToken implements SessionToken {

  private SessionToken[] sessionIds;

  public MultipleSessionToken(SessionToken... sessionIds) {
    this.sessionIds = sessionIds;
  }

  @Override public String findToken(Context ctx) {
    for (SessionToken sessionId : sessionIds) {
      String sid = sessionId.findToken(ctx);
      if (sid != null) {
        return sid;
      }
    }
    return null;
  }

  @Override public void saveToken(Context ctx, String token) {
    strategy(ctx).forEach(it -> it.saveToken(ctx, token));
  }

  @Override public void deleteToken(Context ctx, String token) {
    strategy(ctx).forEach(it -> it.deleteToken(ctx, token));
  }

  private List<SessionToken> strategy(Context ctx) {
    List<SessionToken> result = new ArrayList<>(sessionIds.length);
    for (SessionToken strategy : sessionIds) {
      if (strategy.findToken(ctx) != null) {
        result.add(strategy);
      }
    }
    return result;
  }
}
