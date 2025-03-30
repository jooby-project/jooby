/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3633;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.pac4j.core.util.serializer.JavaSerializer;
import org.pac4j.http.client.direct.HeaderClient;
import org.pac4j.http.client.indirect.FormClient;
import org.pac4j.http.credentials.authenticator.test.SimpleTestTokenAuthenticator;
import org.pac4j.http.credentials.authenticator.test.SimpleTestUsernamePasswordAuthenticator;

import io.jooby.MediaType;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import io.jooby.pac4j.Pac4jModule;
import okhttp3.FormBody;
import okhttp3.Response;

public class Issue3633 {
  private static final String WELCOME =
      "<!DOCTYPE html>\n"
          + "<html>\n"
          + "<head>\n"
          + "  <title>Welcome Page</title>\n"
          + "</head>\n"
          + "<body>\n"
          + "<h3>Welcome: {0}</h3>\n"
          + "<h4><a href=\"/logout\">Logout</a></h4>\n"
          + "</body>\n"
          + "</html>\n";

  @ServerTest
  public void pac4jShouldNotAllowToSetUntrustedDataOnIndirectClients(ServerTestRunner runner) {
    JavaSerializer serializer = new JavaSerializer();
    String externalToken = "b64~" + serializer.serializeToString("hello cwm");
    runner
        .define(
            app -> {
              app.install(
                  new Pac4jModule()
                      .client(
                          conf ->
                              new FormClient(
                                  "/login", new SimpleTestUsernamePasswordAuthenticator())));

              app.get(
                  "/",
                  ctx -> {
                    String token = ctx.query("token").value();
                    ctx.session().put("token", token);
                    Object user = ctx.getUser();
                    return ctx.setResponseType(MediaType.html).send(String.format(WELCOME, user));
                  });
            })
        .dontFollowRedirects()
        .ready(
            http -> {
              http.post(
                  "/callback?client_name=FormClient",
                  new FormBody.Builder().add("username", "test").add("password", "test").build(),
                  rsp -> {
                    String sid = sid(rsp);
                    http.header("Cookie", sid);
                    http.get(
                        "/?token=" + externalToken,
                        rsp2 -> {
                          assertEquals(403, rsp2.code());
                        });

                    // success
                    http.header("Cookie", sid);
                    http.get(
                        "/?token=" + "123",
                        rsp2 -> {
                          assertEquals(200, rsp2.code());
                        });
                  });
            });
  }

  @ServerTest
  public void pac4jShouldNotCreateSessionOnDirectClient(ServerTestRunner runner) {
    JavaSerializer serializer = new JavaSerializer();
    String externalToken = "b64~" + serializer.serializeToString("hello cwm");
    runner
        .define(
            app -> {
              app.install(
                  new Pac4jModule()
                      .client(
                          conf -> new HeaderClient("token", new SimpleTestTokenAuthenticator())));

              app.get(
                  "/",
                  ctx -> {
                    String token = ctx.header("token").value();
                    assertTrue(ctx.sessionOrNull() == null);
                    // force create session, should be OK due pac4j does nothing on direct client
                    // (no session is required)
                    ctx.session().put("token", token);
                    Object user = ctx.getUser();
                    return ctx.setResponseType(MediaType.html).send(String.format(WELCOME, user));
                  });
            })
        .dontFollowRedirects()
        .ready(
            http -> {
              http.header("token", externalToken);
              http.get(
                  "/",
                  rsp2 -> {
                    assertEquals(200, rsp2.code());
                  });
            });
  }

  private String sid(Response response) {
    return response.header("Set-Cookie").split(";")[0];
  }
}
