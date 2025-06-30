/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.SessionToken;

public class MultipleSessionToken implements SessionToken {

  private List<SessionToken> sessionTokens;

  public MultipleSessionToken(SessionToken... sessionToken) {
    this.sessionTokens = Arrays.asList(sessionToken);
  }

  @Override
  public String findToken(@NonNull Context ctx) {
    for (SessionToken sessionToken : sessionTokens) {
      String token = sessionToken.findToken(ctx);
      if (token != null) {
        return token;
      }
    }
    return null;
  }

  @Override
  public void saveToken(@NonNull Context ctx, @NonNull String token) {
    strategy(ctx).forEach(it -> it.saveToken(ctx, token));
  }

  @Override
  public void deleteToken(@NonNull Context ctx, @NonNull String token) {
    strategy(ctx).forEach(it -> it.deleteToken(ctx, token));
  }

  private List<SessionToken> strategy(Context ctx) {
    List<SessionToken> result = new ArrayList<>(sessionTokens.size());
    for (SessionToken sessionToken : sessionTokens) {
      if (sessionToken.findToken(ctx) != null) {
        result.add(sessionToken);
      }
    }
    return result.isEmpty() ? sessionTokens : result;
  }
}
