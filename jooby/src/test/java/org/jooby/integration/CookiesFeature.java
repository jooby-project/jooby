package org.jooby.integration;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Optional;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.jooby.Cookie;
import org.jooby.integration.FilterFeature.HttpResponseValidator;
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
      rsp.cookie(new Cookie.Definition("X", "x").path("/set")).send("done");
    });

    get("/get", (req, rsp) -> {
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
    assertEquals("done", execute(GET(uri("set")), (r1) -> {
      assertEquals(200, r1.getStatusLine().getStatusCode());
      String setCookie = r1.getFirstHeader("Set-Cookie").getValue();
      assertEquals("X=x; path=/set", setCookie);
      execute(GET(uri("get")).addHeader("Cookie", "X=x; $Path=/set"), (r0) -> {
        assertEquals(200, r0.getStatusLine().getStatusCode());
        assertEquals("present", EntityUtils.toString(r0.getEntity()));
      });
    }));

  }

  @Test
  public void noCookies() throws Exception {
    assertEquals("[]", execute(GET(uri("nocookies")), (r1) -> {
      assertEquals(200, r1.getStatusLine().getStatusCode());
    }));

  }

  @Test
  public void clearCookie() throws Exception {
    assertEquals("done", execute(GET(uri("set")), (r0) -> {
      assertEquals(200, r0.getStatusLine().getStatusCode());
      String setCookie = r0.getFirstHeader("Set-Cookie").getValue();
      assertEquals("X=x; path=/set", setCookie);
      execute(GET(uri("clear")).addHeader("Cookie", "X=x;$Path=/clear"), (r1) -> {
        assertEquals(200, r1.getStatusLine().getStatusCode());
        String setCookie2 = r1.getFirstHeader("Set-Cookie").getValue();
        assertEquals("X=x; path=/clear; Max-Age=0; Expires=Thu, 01-Jan-1970 00:00:00 GMT", setCookie2);
      });
    }));

  }

  @Test
  public void listCookies() throws Exception {
    assertEquals(
        "[{name=X, value=Optional[x], domain=Optional.empty, path=/, maxAge=-1, secure=false}]",
        execute(GET(uri("r", "cookies")).addHeader("Cookie", "X=x"), (r1) -> {
          assertEquals(200, r1.getStatusLine().getStatusCode());
        }));

  }

  private static Request GET(final URIBuilder uri) throws Exception {
    return Request.Get(uri.build());
  }

  private static String execute(final Request request, final HttpResponseValidator validator)
      throws Exception {
    HttpResponse resp = request.execute().returnResponse();
    validator.validate(resp);
    return EntityUtils.toString(resp.getEntity());
  }

}
