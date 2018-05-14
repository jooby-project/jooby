package org.jooby.cookies;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.jooby.Cookie;
import org.jooby.Mutant;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
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

  {

    get("/set", (req, rsp) -> {
      Cookie cookie = new Cookie.Definition("X", "x").path("/set").toCookie();
      rsp.cookie(cookie).send(cookie);
    });

    get("/get", (req, rsp) -> {
      // no path for netty
        assertTrue(req.cookies().toString().startsWith("[X=x;Version=1"));
        Mutant cookie = req.cookie("X");
        rsp.send(cookie.isSet() ? "present" : "deleted");
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
        .expect("X=x;Version=1;Path=/set")
        .header("Set-Cookie", setCookie -> {
          assertEquals("X=x;Version=1;Path=/set", setCookie);
          request()
              .get("/get")
              .header("Cookie", "$Version=1; X=x; $Path=/set;")
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
        .expect("X=x;Version=1;Path=/set")
        .header("Set-Cookie",
            setCookie -> {
              assertEquals(setCookie, "X=x;Version=1;Path=/set");
              request()
                  .get("/clear")
                  .header("Cookie", "X=x; $Path=/clear; $Version=1")
                  .expect(200)
                  .header("Set-Cookie",
                      "X=;Version=1;Path=/;Max-Age=0;Expires=Thu, 01-Jan-1970 00:00:00 GMT");
            });

  }

  @Test
  public void listCookies() throws Exception {
    request()
        .get("/r/cookies")
        .header("Cookie", "X=x")
        .expect(200)
        .expect("[X=x;Version=1]");

  }

}
