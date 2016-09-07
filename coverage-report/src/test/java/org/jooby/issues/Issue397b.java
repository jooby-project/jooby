package org.jooby.issues;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Optional;

import org.jooby.FlashScope;
import org.jooby.mvc.Flash;
import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue397b extends ServerFeature {

  @Path("/397")
  public static class Resource {

    @GET
    @Path("/flash")
    public Object flash(@Flash final Map<String, String> flash) {
      flash.put("foo", "bar");
      return flash;
    }

    @GET
    @Path("/flash/attr")
    public Object flash(@Flash final String foo) {
      return foo;
    }

    @GET
    @Path("/flash/attr/optional")
    public Object flash(@Flash final Optional<String> foo) {
      return foo.orElse("empty");
    }
  }

  {
    use(new FlashScope());

    use(Resource.class);

    err((req, rsp, err) -> {
      err.printStackTrace();
      rsp.send(err.statusCode() + ": " + err.getCause().getMessage());
    });
  }

  @Test
  public void flashScopeOnMvc() throws Exception {
    request()
        .get("/397/flash")
        .expect("{foo=bar}")
        .header("Set-Cookie", setCookie -> {
          assertEquals("jooby.flash=foo=bar;Version=1;Path=/;HttpOnly", setCookie);
          request()
              .get("/397/flash/attr")
              .expect("bar")
              .header("Set-Cookie", clearCookie -> {
                assertTrue(clearCookie.startsWith("jooby.flash=;Version=1;Path=/;HttpOnly;Max-Age=0;"));
              });
        });
  }

  @Test
  public void optionalFlashScope() throws Exception {
    request()
        .get("/397/flash/attr/optional")
        .header("Cookie", "")
        .expect("empty");
  }

}
