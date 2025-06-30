/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import io.jooby.Cookie;
import io.jooby.Session;
import io.jooby.SessionStore;
import io.jooby.SessionToken;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import io.jooby.jwt.JwtSessionStore;
import okhttp3.Response;

public class SessionTest {
  private static final Cookie SID = Cookie.session("jooby.sid");

  @ServerTest
  public void sessionIdAsCookie(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.get("/findSession", ctx -> Optional.ofNullable(ctx.sessionOrNull()).isPresent());
              app.get("/getSession", ctx -> ctx.session().get("foo").value("none"));
              app.get("/putSession", ctx -> ctx.session().put("foo", "bar").get("foo").value());
              app.get(
                  "/destroySession",
                  ctx -> {
                    Session session = ctx.session();
                    session.destroy();
                    return Optional.ofNullable(ctx.sessionOrNull()).isPresent();
                  });
            })
        .ready(
            client -> {
              client.get(
                  "/findSession",
                  rsp -> {
                    assertEquals("[]", rsp.headers("Set-Cookie").toString());
                    assertEquals("false", rsp.body().string());
                  });
              client.header("Cookie", "jooby.sid=1234missing");
              client.get(
                  "/findSession",
                  rsp -> {
                    assertEquals("[]", rsp.headers("Set-Cookie").toString());
                    assertEquals("false", rsp.body().string());
                  });

              client.get(
                  "/getSession",
                  rsp -> {
                    assertEquals("none", rsp.body().string());
                    String sid = sid(rsp, "jooby.sid=");

                    client.header("Cookie", "jooby.sid=" + sid);
                    client.get(
                        "/findSession",
                        findSession -> {
                          assertEquals(
                              "[jooby.sid=" + sid + ";Path=/;HttpOnly]",
                              findSession.headers("Set-Cookie").toString());
                          assertEquals("true", findSession.body().string());
                        });
                    client.header("Cookie", "jooby.sid=" + sid);
                    client.get(
                        "/putSession",
                        putSession -> {
                          assertEquals(
                              "[jooby.sid=" + sid + ";Path=/;HttpOnly]",
                              putSession.headers("Set-Cookie").toString());
                          assertEquals("bar", putSession.body().string());
                        });
                    client.header("Cookie", "jooby.sid=" + sid);
                    client.get(
                        "/getSession",
                        putSession -> {
                          assertEquals(
                              "[jooby.sid=" + sid + ";Path=/;HttpOnly]",
                              putSession.headers("Set-Cookie").toString());
                          assertEquals("bar", putSession.body().string());
                        });
                    client.header("Cookie", "jooby.sid=" + sid);
                    client.get(
                        "/destroySession",
                        putSession -> {
                          assertEquals(
                              "jooby.sid="
                                  + sid(rsp, "jooby.sid=")
                                  + ";Path=/;HttpOnly;Max-Age=0;Expires=Thu, 01-Jan-1970 00:00:00"
                                  + " GMT",
                              putSession.headers("Set-Cookie").get(0));
                          assertEquals("false", putSession.body().string());
                        });
                    client.header("Cookie", "jooby.sid=" + sid);
                    client.get(
                        "/findSession",
                        putSession -> {
                          assertEquals("[]", putSession.headers("Set-Cookie").toString());
                          assertEquals("false", putSession.body().string());
                        });
                  });
            });
  }

  @ServerTest
  public void sessionIdAsCookieMaxAge(ServerTestRunner runner) {
    /**********************************************************************************************/
    // Max Age
    /**********************************************************************************************/
    runner
        .define(
            app -> {
              app.setSessionStore((SessionStore.memory(new Cookie("my.sid").setMaxAge(1L))));
              app.get("/session", ctx -> ctx.session().toMap());
              app.get(
                  "/sessionMaxAge", ctx -> Optional.ofNullable(ctx.sessionOrNull()).isPresent());
            })
        .ready(
            client -> {
              client.get(
                  "/session",
                  rsp -> {
                    String setCookie = rsp.header("Set-Cookie");
                    assertTrue(setCookie.startsWith("my.sid"));
                    assertTrue(setCookie.contains(";Max-Age=1;"));
                  });
            });
  }

  @ServerTest
  public void cookieDataSession(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.setSessionStore(SessionStore.signed(SID, "ABC123"));

              app.get(
                  "/session",
                  ctx -> {
                    Session session = ctx.session();
                    session.put("foo", "bar");
                    return ctx.getRequestPath();
                  });

              app.get(
                  "/sessionOrNull",
                  ctx -> {
                    Session session = ctx.sessionOrNull();
                    return session == null ? "no-session" : session.toMap();
                  });

              app.get(
                  "/destroy",
                  ctx -> {
                    ctx.session().destroy();
                    return ctx.getRequestPath();
                  });
            })
        .ready(
            client -> {
              String signedCookie =
                  "jooby.sid=/Msfofr9BlBU4ftLP6hPwZQMeozEWmaX4tFr4gOz4cU|foo=bar;Path=/;HttpOnly";
              client.get(
                  "/session",
                  rsp -> {
                    assertEquals(signedCookie, rsp.header("Set-Cookie"));
                  });
              // Always write cookie back
              client.header("Cookie", signedCookie);
              client.get(
                  "/session",
                  rsp -> {
                    assertEquals(signedCookie, rsp.header("Set-Cookie"));
                  });
              client.header("Cookie", signedCookie);
              client.get(
                  "/sessionOrNull",
                  rsp -> {
                    assertEquals(null, rsp.header("Set-Cookie"));
                    assertEquals("{foo=bar}", rsp.body().string());
                  });
              // Tampering silent ignore the cookie
              client.header(
                  "Cookie",
                  "jooby.sid=/Msfofr9BlBU4ftLP6hPwZQMeozEWmaX4tFr4gOz4cU|bar=foo;Path=/;HttpOnly");
              client.get(
                  "/sessionOrNull",
                  rsp -> {
                    assertEquals(null, rsp.header("Set-Cookie"));
                    assertEquals("no-session", rsp.body().string());
                  });
              // destroy session
              client.header("Cookie", signedCookie);
              client.get(
                  "/destroy",
                  rsp -> {
                    assertEquals(
                        "jooby.sid=;Path=/;HttpOnly;Max-Age=0;Expires=Thu, 01-Jan-1970 00:00:00"
                            + " GMT",
                        rsp.header("Set-Cookie"));
                  });
            });
  }

  @ServerTest
  public void sessionIdHeader(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.setSessionStore((SessionStore.memory(SessionToken.header("jooby.sid"))));

              app.get("/findSession", ctx -> Optional.ofNullable(ctx.sessionOrNull()).isPresent());
              app.get("/getSession", ctx -> ctx.session().get("foo").value("none"));
              app.get("/putSession", ctx -> ctx.session().put("foo", "bar").get("foo").value());
              app.get(
                  "/destroySession",
                  ctx -> {
                    Session session = ctx.session();
                    session.destroy();

                    return Optional.ofNullable(ctx.sessionOrNull()).isPresent();
                  });
            })
        .ready(
            client -> {
              client.get(
                  "/findSession",
                  rsp -> {
                    assertEquals(null, rsp.header("jooby.sid"));
                    assertEquals("false", rsp.body().string());
                  });
              client.header("jooby.sid", "1234missing");
              client.get(
                  "/findSession",
                  rsp -> {
                    assertEquals(null, rsp.header("jooby.sid"));
                    assertEquals("false", rsp.body().string());
                  });

              client.get(
                  "/getSession",
                  rsp -> {
                    assertEquals("none", rsp.body().string());
                    String sid = rsp.header("jooby.sid");

                    client.header("jooby.sid", sid);
                    client.get(
                        "/findSession",
                        findSession -> {
                          assertEquals(sid, findSession.header("jooby.sid"));
                          assertEquals("true", findSession.body().string());
                        });
                    client.header("jooby.sid", sid);
                    client.get(
                        "/putSession",
                        putSession -> {
                          assertEquals(sid, putSession.header("jooby.sid"));
                          assertEquals("bar", putSession.body().string());
                        });
                    client.header("jooby.sid", sid);
                    client.get(
                        "/getSession",
                        putSession -> {
                          assertEquals(sid, putSession.header("jooby.sid"));
                          assertEquals("bar", putSession.body().string());
                        });
                    client.header("jooby.sid", sid);
                    client.get(
                        "/destroySession",
                        putSession -> {
                          assertEquals(null, putSession.header("jooby.sid"));
                          assertEquals("false", putSession.body().string());
                        });
                    client.header("jooby.sid", sid);
                    client.get(
                        "/findSession",
                        putSession -> {
                          assertEquals(null, putSession.header("jooby.sid"));
                          assertEquals("false", putSession.body().string());
                        });
                  });
            });
  }

  @ServerTest
  public void sessionIdMultiple(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              SessionToken token =
                  SessionToken.combine(
                      SessionToken.header("TOKEN"),
                      SessionToken.cookieId(SID.clone().setMaxAge(Duration.ofMinutes(30))));

              app.setSessionStore((SessionStore.memory(token)));

              app.get("/session", ctx -> ctx.session().getId());
            })
        .ready(
            client -> {
              client.get(
                  "/session",
                  rsp -> {
                    // Cookie version
                    String sid = sid(rsp, "jooby.sid=");
                    assertNotNull(sid);
                    String header = rsp.header("TOKEN");
                    assertNotNull(header);
                    assertEquals(sid, header);

                    client.header("Cookie", "jooby.sid=" + sid);
                    client.get(
                        "/session",
                        sessionCookie -> {
                          assertEquals(sid, sessionCookie.body().string());
                          assertNotNull(sessionCookie.header("Set-Cookie"));
                          assertNull(sessionCookie.header("TOKEN"));
                        });

                    client.header("TOKEN", sid);
                    client.get(
                        "/session",
                        headerCookie -> {
                          assertEquals(sid, headerCookie.body().string());
                          assertNotNull(headerCookie.header("TOKEN"));
                          assertNull(headerCookie.header("Set-Cookie"));
                        });
                  });
            });
  }

  @ServerTest
  public void sessionData(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.get(
                  "/session",
                  ctx -> ctx.session().put("foo", "1").put("e", ChronoUnit.DAYS.name()).toMap());

              app.get("/session/convert", ctx -> ctx.session().get("e").to(ChronoUnit.class));
            })
        .ready(
            client -> {
              client.get(
                  "/session",
                  rsp -> {
                    // Cookie version
                    String sid = sid(rsp, "jooby.sid=");
                    assertNotNull(sid);

                    client.header("Cookie", "jooby.sid=" + sid);
                    client.get(
                        "/session/convert",
                        convert -> {
                          assertEquals("Days", convert.body().string());
                        });
                  });
            });
  }

  @ServerTest
  public void jsonwebtokenSession(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.setSessionStore(
                  new JwtSessionStore(
                      SessionToken.cookieId(SID), "7a85c3b6-3ef0-4625-82d3-a1da36094804"));
              app.get(
                  "/session",
                  ctx -> {
                    Session session = ctx.session();
                    session.put("foo", "bar");
                    return session.toMap();
                  });

              app.get(
                  "/ifsession",
                  ctx -> {
                    Session session = ctx.sessionOrNull();
                    return session == null ? "<>" : session.toMap();
                  });

              app.get(
                  "/destroy",
                  ctx -> {
                    ctx.session().destroy();
                    return "destroy";
                  });
            })
        .ready(
            client -> {
              client.get(
                  "/session",
                  rsp -> {
                    assertEquals("{foo=bar}", rsp.body().string());

                    String sid = sid(rsp, "jooby.sid=");
                    client.header("Cookie", "jooby.sid=" + sid);
                    client.get(
                        "/ifsession",
                        ifsession -> {
                          assertEquals("{foo=bar}", ifsession.body().string());
                        });

                    // Tampering silent ignore the cookie
                    client.header("Cookie", "jooby.sid=" + sid + "x");
                    client.get(
                        "/ifsession",
                        ifsession -> {
                          assertEquals("<>", ifsession.body().string());
                        });

                    client.header("Cookie", "jooby.sid=" + sid);
                    client.get(
                        "/destroy",
                        destroy -> {
                          assertEquals(
                              "[jooby.sid=;Path=/;HttpOnly;Max-Age=0;Expires=Thu, 01-Jan-1970"
                                  + " 00:00:00 GMT]",
                              destroy.headers("Set-Cookie").toString());
                        });
                  });
            });
  }

  private String sid(Response rsp, String prefix) {
    String setCookie = rsp.header("Set-Cookie");
    assertNotNull(setCookie);
    assertTrue(setCookie.startsWith(prefix));
    return setCookie.substring(prefix.length(), setCookie.indexOf(";"));
  }
}
