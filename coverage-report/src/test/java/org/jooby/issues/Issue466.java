package org.jooby.issues;

import org.jooby.Results;
import org.jooby.Session;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class Issue466 extends ServerFeature {

  {

    use(ConfigFactory.empty().withValue("application.secret",
        ConfigValueFactory.fromAnyRef("1234Querty")));

    cookieSession()
        .cookie()
        .maxAge(86400)
        .name("user");

    get("/466/home", req -> {
      Session session = req.session();
      return session.attributes();
    });

    get("/466/nosession", req -> {
      return "OK";
    });

    get("/466/emptysession", req -> {
      Session session = req.session();
      return session.attributes();
    });

    get("/466/session", req -> {
      Session session = req.session();
      session.set("foo", "bar");
      return session.attributes();
    });

    get("/466/destroy", req -> {
      req.session().destroy();
      return Results.redirect("/466/home");
    });

  }

  @Test
  public void shouldDestroyAllSessionData() throws Exception {
    request()
        .dontFollowRedirect()
        .get("/466/session")
        .expect("{foo=bar}");

    request()
        .dontFollowRedirect()
        .get("/466/destroy")
        .execute()
        .header("Set-Cookie", "user=;Version=1;Path=/;HttpOnly;Max-Age=0;Expires=Thu, 01-Jan-1970 00:00:00 GMT");
  }

  @Test
  public void shouldNotCreateSessionCookie() throws Exception {
    request()
        .dontFollowRedirect()
        .get("/466/nosession")
        .execute()
        .header("Set-Cookie", (String) null);
  }

  @Test
  public void shouldNotCreateEmptySessionCookie() throws Exception {
    request()
        .dontFollowRedirect()
        .get("/466/emptysession")
        .execute()
        .header("Set-Cookie", (String) null);
  }

  @Test
  public void shouldDestroyAllSessionDataFollowRedirect() throws Exception {
    request()
        .get("/466/session")
        .expect("{foo=bar}");

    request()
        .get("/466/destroy")
        .expect("{}");
  }
}
