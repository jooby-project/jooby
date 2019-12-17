package io.jooby;

import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HttpsTest {

  @ServerTest
  public void httpsPkcs12(ServerTestRunner runner) {
    runner.define(app -> {

      app.setServerOptions(new ServerOptions().setSecurePort(8433));
      app.get("/",
          ctx -> "schema: " + ctx.getScheme() + "; protocol: " + ctx.getProtocol() + "; secure: "
              + ctx.isSecure() + "; ssl: " + app.getServerOptions().getSsl().getType());
    }).ready((http, https) -> {
      https.get("/", rsp -> {
        assertEquals("schema: https; protocol: HTTP/1.1; secure: true; ssl: PKCS12",
            rsp.body().string());
      });
      http.get("/", rsp -> {
        assertEquals("schema: http; protocol: HTTP/1.1; secure: false; ssl: PKCS12",
            rsp.body().string());
      });
    });
  }

  @ServerTest
  public void httpsX509(ServerTestRunner runner) {
    runner.define(app -> {

      SslOptions options = SslOptions.selfSigned(SslOptions.X509);
      app.setServerOptions(new ServerOptions().setSsl(options));
      app.get("/",
          ctx -> "schema: " + ctx.getScheme() + "; protocol: " + ctx.getProtocol() + "; secure: "
              + ctx.isSecure() + "; ssl: " + app.getServerOptions().getSsl().getType());
    }).ready((http, https) -> {
      https.get("/", rsp -> {
        assertEquals("schema: https; protocol: HTTP/1.1; secure: true; ssl: X509",
            rsp.body().string());
      });
      http.get("/", rsp -> {
        assertEquals("schema: http; protocol: HTTP/1.1; secure: false; ssl: X509",
            rsp.body().string());
      });
    });
  }

  @ServerTest
  public void forceSSL(ServerTestRunner runner) {
    runner.define(app -> {

      app.setContextPath("/secure");

      app.setServerOptions(new ServerOptions().setSecurePort(8433));

      app.before(new SSLHandler(true));

      app.get("/{path}", ctx -> ctx.getRequestPath());
    }).dontFollowRedirects().ready((http, https) -> {
      http.get("/secure/path", rsp -> {
        assertEquals("https://localhost:" + https.getPort() + "/secure/path",
            rsp.header("Location"));
        assertEquals(302, rsp.code());
      });

      http.header("X-Forwarded-Host", "myhost.org");
      http.get("/secure/path?a=b", rsp -> {
        assertEquals("https://myhost.org/secure/path?a=b",
            rsp.header("Location"));
        assertEquals(302, rsp.code());
      });
    });
  }

  @ServerTest
  public void forceSSL2(ServerTestRunner runner) {
    runner.define(app -> {

      app.setServerOptions(new ServerOptions().setSecurePort(8433));

      app.before(new SSLHandler(true));

      app.get("/{path}", ctx -> ctx.getRequestPath());
    }).dontFollowRedirects().ready((http, https) -> {
      http.get("/path", rsp -> {
        assertEquals("https://localhost:" + https.getPort() + "/path",
            rsp.header("Location"));
        assertEquals(302, rsp.code());
      });

      http.header("X-Forwarded-Host", "myhost.org");
      http.get("/path?a=b", rsp -> {
        assertEquals("https://myhost.org/path?a=b",
            rsp.header("Location"));
        assertEquals(302, rsp.code());
      });
    });

  }

  @ServerTest
  public void forceSSLStatic(ServerTestRunner runner) {
    runner.define(app -> {

      app.setServerOptions(new ServerOptions().setSecurePort(8433));

      app.before(new SSLHandler("static.org"));

      app.get("/{path}", ctx -> ctx.getRequestPath());
    }).dontFollowRedirects().ready(http -> {
      http.get("/path", rsp -> {
        assertEquals("https://static.org/path",
            rsp.header("Location"));
        assertEquals(302, rsp.code());
      });

      http.header("X-Forwarded-Host", "myhost.org");
      http.get("/path?a=b", rsp -> {
        assertEquals("https://static.org/path?a=b",
            rsp.header("Location"));
        assertEquals(302, rsp.code());
      });
    });
  }

  @ServerTest
  public void forceSSLStatic2(ServerTestRunner runner) {
    runner.define(app -> {
      app.setContextPath("/ppp");
      app.setServerOptions(new ServerOptions().setSecurePort(8433));

      app.before(new SSLHandler("static.org"));

      app.get("/{path}", ctx -> ctx.getRequestPath());
    }).dontFollowRedirects().ready(http -> {
      http.get("/ppp/path", rsp -> {
        assertEquals("https://static.org/ppp/path",
            rsp.header("Location"));
        assertEquals(302, rsp.code());
      });

      http.header("X-Forwarded-Host", "myhost.org");
      http.get("/ppp/path?a=b", rsp -> {
        assertEquals("https://static.org/ppp/path?a=b",
            rsp.header("Location"));
        assertEquals(302, rsp.code());
      });
    });
  }
}
