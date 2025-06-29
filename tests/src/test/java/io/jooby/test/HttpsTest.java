/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import static org.junit.jupiter.api.Assertions.*;

import io.jooby.ServerOptions;
import io.jooby.SslOptions;
import io.jooby.handler.SSLHandler;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class HttpsTest {

  @ServerTest
  public void httpsPkcs12(ServerTestRunner runner) {
    runner
        .options(new ServerOptions().setSecurePort(8443))
        .define(
            app -> {
              app.get(
                  "/",
                  ctx ->
                      "schema: "
                          + ctx.getScheme()
                          + "; protocol: "
                          + ctx.getProtocol()
                          + "; secure: "
                          + ctx.isSecure()
                          + "; ssl: "
                          + ctx.require(ServerOptions.class).getSsl().getType());
            })
        .ready(
            (http, https) -> {
              https.get(
                  "/",
                  rsp -> {
                    assertEquals(
                        "schema: https; protocol: HTTP/1.1; secure: true; ssl: PKCS12",
                        rsp.body().string());
                  });
              http.get(
                  "/",
                  rsp -> {
                    assertEquals(
                        "schema: http; protocol: HTTP/1.1; secure: false; ssl: PKCS12",
                        rsp.body().string());
                  });
            });
  }

  @ServerTest
  public void httpsX509(ServerTestRunner runner) {
    SslOptions options = SslOptions.selfSigned(SslOptions.X509);
    runner
        .options(new ServerOptions().setSsl(options))
        .define(
            app -> {
              app.get(
                  "/",
                  ctx ->
                      "schema: "
                          + ctx.getScheme()
                          + "; protocol: "
                          + ctx.getProtocol()
                          + "; secure: "
                          + ctx.isSecure()
                          + "; ssl: "
                          + ctx.require(ServerOptions.class).getSsl().getType());
            })
        .ready(
            (http, https) -> {
              https.get(
                  "/",
                  rsp -> {
                    assertEquals(
                        "schema: https; protocol: HTTP/1.1; secure: true; ssl: X509",
                        rsp.body().string());
                  });
              http.get(
                  "/",
                  rsp -> {
                    assertEquals(
                        "schema: http; protocol: HTTP/1.1; secure: false; ssl: X509",
                        rsp.body().string());
                  });
            });
  }

  @ServerTest
  public void forceSSL(ServerTestRunner runner) {
    runner
        .options(new ServerOptions().setSecurePort(8443))
        .define(
            app -> {
              app.setTrustProxy(true);
              app.setContextPath("/secure");

              app.before(new SSLHandler());

              app.get("/{path}", ctx -> ctx.getRequestPath());
            })
        .dontFollowRedirects()
        .ready(
            (http, https) -> {
              http.get(
                  "/secure/path",
                  rsp -> {
                    assertEquals(
                        "https://localhost:" + https.getPort() + "/secure/path",
                        rsp.header("Location"));
                    assertEquals(302, rsp.code());
                  });

              http.header("X-Forwarded-Host", "myhost.org");
              http.get(
                  "/secure/path?a=b",
                  rsp -> {
                    assertEquals("https://myhost.org/secure/path?a=b", rsp.header("Location"));
                    assertEquals(302, rsp.code());
                  });
            });
  }

  @ServerTest
  public void forceSSL2(ServerTestRunner runner) {
    runner
        .options(new ServerOptions().setSecurePort(8443))
        .define(
            app -> {
              app.setTrustProxy(true);

              app.before(new SSLHandler());

              app.get("/{path}", ctx -> ctx.getRequestPath());
            })
        .dontFollowRedirects()
        .ready(
            (http, https) -> {
              http.get(
                  "/path",
                  rsp -> {
                    assertEquals(
                        "https://localhost:" + https.getPort() + "/path", rsp.header("Location"));
                    assertEquals(302, rsp.code());
                  });

              http.header("X-Forwarded-Host", "myhost.org");
              http.get(
                  "/path?a=b",
                  rsp -> {
                    assertEquals("https://myhost.org/path?a=b", rsp.header("Location"));
                    assertEquals(302, rsp.code());
                  });
            });
  }

  @ServerTest
  public void forceSSLStatic(ServerTestRunner runner) {
    runner
        .options(new ServerOptions().setSecurePort(8443))
        .define(
            app -> {
              app.before(new SSLHandler("static.org"));

              app.get("/{path}", ctx -> ctx.getRequestPath());
            })
        .dontFollowRedirects()
        .ready(
            http -> {
              http.get(
                  "/path",
                  rsp -> {
                    assertEquals("https://static.org/path", rsp.header("Location"));
                    assertEquals(302, rsp.code());
                  });

              http.header("X-Forwarded-Host", "myhost.org");
              http.get(
                  "/path?a=b",
                  rsp -> {
                    assertEquals("https://static.org/path?a=b", rsp.header("Location"));
                    assertEquals(302, rsp.code());
                  });
            });
  }

  @ServerTest
  public void forceSSLStatic2(ServerTestRunner runner) {
    runner
        .options(new ServerOptions().setSecurePort(8443))
        .define(
            app -> {
              app.setContextPath("/ppp");

              app.before(new SSLHandler("static.org"));

              app.get("/{path}", ctx -> ctx.getRequestPath());
            })
        .dontFollowRedirects()
        .ready(
            http -> {
              http.get(
                  "/ppp/path",
                  rsp -> {
                    assertEquals("https://static.org/ppp/path", rsp.header("Location"));
                    assertEquals(302, rsp.code());
                  });

              http.header("X-Forwarded-Host", "myhost.org");
              http.get(
                  "/ppp/path?a=b",
                  rsp -> {
                    assertEquals("https://static.org/ppp/path?a=b", rsp.header("Location"));
                    assertEquals(302, rsp.code());
                  });
            });
  }

  @ServerTest
  public void httpsOnly(ServerTestRunner runner) {
    runner
        .options(new ServerOptions().setSecurePort(8443).setHttpsOnly(true))
        .define(
            app -> {
              app.get("/test", ctx -> "test");
            })
        .ready(
            (http, https) -> {
              https.get("/test", rsp -> assertEquals("test", rsp.body().string()));
            });
  }

  @ServerTest
  public void customSslContext(ServerTestRunner runner) {
    runner
        .options(
            new ServerOptions()
                .setSecurePort(8443)
                .setHttpsOnly(true)
                .setSsl(SslOptions.selfSigned()))
        .define(
            app -> {
              var options = app.require(ServerOptions.class);
              options.setSsl(SslOptions.selfSigned());
              // a fresh context is created every time based on config
              var ctx1 = options.getSSLContext(this.getClass().getClassLoader());
              var ctx2 = options.getSSLContext(this.getClass().getClassLoader());
              assertNotSame(ctx1, ctx2);

              // now always the configured context is returned
              options.getSsl().setSslContext(ctx1);
              assertSame(ctx1, options.getSSLContext(this.getClass().getClassLoader()));
              assertSame(ctx1, options.getSSLContext(this.getClass().getClassLoader()));

              app.get("/test", ctx -> "test");
            })
        .ready(
            (http, https) -> {
              https.get("/test", rsp -> assertEquals("test", rsp.body().string()));
            });
  }
}
