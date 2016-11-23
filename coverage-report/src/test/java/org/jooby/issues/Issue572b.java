package org.jooby.issues;

import static org.junit.Assert.assertEquals;

import org.jooby.pac4j.Auth;
import org.jooby.test.ServerFeature;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;

public class Issue572b extends ServerFeature {

  {
    use(new Auth());

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
          String action = (html.select("form").attr("action"));
          assertEquals("/auth?client_name=FormClient", action);
        });
  }

}
