package io.jooby;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HttpsTest {

  @Test
  public void httpsPkcs12() {
    new JoobyRunner(app -> {

      app.setServerOptions(new ServerOptions().setSecurePort(8433));
      app.get("/",
          ctx -> "schema: " + ctx.getScheme() + "; protocol: " + ctx.getProtocol() + "; secure: "
              + ctx.isSecure() + "; ssl: " + app.getServerOptions().getSsl().getType());
    }).ready((http, https, server) -> {
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

  @Test
  public void httpsX509() {
    new JoobyRunner(app -> {

      SslOptions options = SslOptions.selfSigned(SslOptions.X509);
      app.setServerOptions(new ServerOptions().setSsl(options));
      app.get("/",
          ctx -> "schema: " + ctx.getScheme() + "; protocol: " + ctx.getProtocol() + "; secure: "
              + ctx.isSecure() + "; ssl: " + app.getServerOptions().getSsl().getType());
    }).ready((http, https, server) -> {
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

  @Test
  public void forceSSL() {
    new JoobyRunner(app -> {

      app.setServerOptions(new ServerOptions().setSecurePort(8433));

      app.before(new SSLHandler(true));

      app.get("/{path}", ctx -> ctx.pathString());
    }).dontFollowRedirects().ready(http -> {
      http.get("/path", rsp -> {
        assertEquals("https://localhost:9443/path",
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

  @Test
  public void forceSSLStatic() {
    new JoobyRunner(app -> {

      app.setServerOptions(new ServerOptions().setSecurePort(8433));

      app.before(new SSLHandler("static.org"));

      app.get("/{path}", ctx -> ctx.pathString());
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
}
