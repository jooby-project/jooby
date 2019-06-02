/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import io.jooby.ExecutionMode;
import io.jooby.Jooby;
import io.jooby.Session;
import io.jooby.SessionOptions;

public class SessionApp extends Jooby {

  {
    setSessionOptions(new SessionOptions());
    get("/exists", ctx -> ctx.sessionOrNull() != null);

    get("/create", ctx -> {
      Session session = ctx.session();
      ctx.queryMap().forEach(session::put);
      return session.toMap();
    });
  }

  public static void main(String[] args) {
    runApp(args, ExecutionMode.EVENT_LOOP, SessionApp::new);
  }
}
