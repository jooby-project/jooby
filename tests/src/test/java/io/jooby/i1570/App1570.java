/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i1570;

import io.jooby.Jooby;
import io.jooby.MediaType;
import io.jooby.Session;
import io.jooby.SessionToken;
import io.jooby.jwt.JwtSessionStore;

public class App1570 extends Jooby {
  {
    String secret = "9968518B15AD9DCD1B33B54316416341CA518B15AD";
    setSessionStore(new JwtSessionStore(SessionToken.header("sid"), secret));

    get(
        "/registerClient/{name}",
        ctx -> {
          Session session = ctx.session();
          String engine = ctx.path("name").value();
          session.put("handle", engine);
          ctx.setResponseType(MediaType.json);
          return String.format("{\"clientName\": \"%s\"}", engine);
        });

    get(
        "/clientName",
        ctx -> {
          String name = ctx.session().get("handle").value();
          ctx.setResponseType(MediaType.json);
          return String.format("{\"clientName\": \"%s\"}", name);
        });
  }
}
