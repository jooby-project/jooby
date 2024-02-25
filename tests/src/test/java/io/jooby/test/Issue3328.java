/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.pac4j.core.credentials.TokenCredentials;
import org.pac4j.core.profile.BasicUserProfile;
import org.pac4j.http.client.direct.HeaderClient;

import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import io.jooby.pac4j.Pac4jModule;
import io.jooby.pac4j.Pac4jOptions;

public class Issue3328 {

  @ServerTest
  public void pac4jShouldWorkWithSignedSession(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.install(
                  new Pac4jModule(new Pac4jOptions())
                      .client(
                          conf ->
                              new HeaderClient(
                                  "user-id",
                                  (credentials, context, sessionStore) -> {
                                    var profile = new BasicUserProfile();
                                    profile.setId(((TokenCredentials) credentials).getToken());
                                    credentials.setUserProfile(profile);
                                  })));

              app.get("/i3328", ctx -> ((BasicUserProfile) ctx.getUser()).getId());
            })
        .ready(
            http -> {
              var userid = UUID.randomUUID().toString();
              http.header("user-id", userid)
                  .get(
                      "/i3328",
                      rsp -> {
                        assertEquals(userid, rsp.body().string());
                      });
            });
  }
}
