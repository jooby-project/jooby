package org.jooby.pac4j;

import org.jooby.test.ServerFeature;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FormAuthFeature extends ServerFeature {

  {

    use(new Auth().form());

    get("/", req -> req.path());
  }

  @Test
  public void auth() throws Exception {
    request()
        .get("/auth?username=test&password=test")
        .expect("/");
  }

  @Test
  public void redirectToLoginPage() throws Exception {
    request()
        .dontFollowRedirect()
        .get("/auth/form")
        .expect(302)
        .header("Location", "/login");
  }

  @Test
  public void loginPage() throws Exception {
    request()
        .get("/auth/form")
        .expect(rsp -> {
          Document html = Jsoup.parse(rsp);
          assertEquals("Login Page", html.getElementsByTag("title").iterator().next().text());
        });
  }

}
