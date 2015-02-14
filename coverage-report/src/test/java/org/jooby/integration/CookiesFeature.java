package org.jooby.integration;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Optional;

import org.jooby.Cookie;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Before;
import org.junit.Test;

public class CookiesFeature extends ServerFeature {

  @Path("r")
  public static class Resource {

    @org.jooby.mvc.GET
    @Path("cookies")
    public String list(final List<Cookie> cookies) {
      return cookies.toString();
    }
  }

  @Before
  public void debug() {
    java.util.logging.Logger.getLogger("httpclient.wire.header").setLevel(
        java.util.logging.Level.FINEST);
    // java.util.logging.Logger.getLogger("httpclient.wire.content").setLevel(java.util.logging.Level.FINEST);

    System.setProperty("org.apache.commons.logging.Log",
        "org.apache.commons.logging.impl.SimpleLog");
    System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
    System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "debug");
    System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "debug");
    System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.headers", "debug");
  }

  {

    get("/set", (req, rsp) -> {
      rsp.cookie(new Cookie.Definition("X", "x").path("/set")).send("done");
    });

    get("/get",
        (req, rsp) -> {
          System.out.println(req.cookies());
          assertEquals(
              "[{name=X, value=Optional[x], domain=Optional.empty, path=/set, maxAge=-1, secure=false}]",
              req.cookies().toString());
          Optional<Cookie> cookie = req.cookie("X");
          rsp.send(cookie.isPresent() ? "present" : "deleted");
        });

    get("/nocookies", (req, rsp) -> {
      rsp.send(req.cookies().toString());
    });

    get("/clear", (req, rsp) -> {
      rsp.clearCookie("X");
      rsp.status(200);
    });

    use(Resource.class);

  }

  @Test
  public void responseCookie() throws Exception {
    request()
        .get("/set")
        .expect(200)
        .header("Set-Cookie", setCookie -> {
          assertEquals("X=x; path=/set", setCookie);
          request()
              .get("/get")
              .header("Cookie", "X=x; $Path=/set")
              .expect(200)
              .expect("present");
        });
  }

  @Test
  public void noCookies() throws Exception {
    request()
        .get("/nocookies")
        .expect(200)
        .expect("[]");
  }

  @Test
  public void clearCookie() throws Exception {
    request()
        .get("/set")
        .expect(200)
        .header("Set-Cookie", setCookie -> {
          assertEquals("X=x; path=/set", setCookie);
          request()
              .get("/clear")
              .header("Cookie", "X=x; $Path=/clear")
              .expect(200)
              .header("Set-Cookie", expiredCookie -> {
                assertEquals("X=x; path=/clear; Max-Age=0; Expires=Thu, 01-Jan-1970 00:00:00 GMT",
                    expiredCookie);
              });
        });

  }

  @Test
  public void listCookies() throws Exception {
    request()
        .get("/r/cookies")
        .header("Cookie", "X=x")
        .expect(200)
        .expect(
            "[{name=X, value=Optional[x], domain=Optional.empty, path=/, maxAge=-1, secure=false}]");

  }

}
