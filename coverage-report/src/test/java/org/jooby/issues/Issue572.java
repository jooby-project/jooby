package org.jooby.issues;

import static org.junit.Assert.assertEquals;

import org.jooby.pac4j.Auth;
import org.jooby.test.ServerFeature;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class Issue572 extends ServerFeature {

  {
    use(ConfigFactory.empty()
        .withValue("application.path", ConfigValueFactory.fromAnyRef("/572")));

    use(new Auth());

    get("/", req -> req.path());
  }

  @Test
  public void auth() throws Exception {
    request()
        .get("/572/auth?username=test&password=test")
        .expect("/");
  }

  @Test
  public void redirectToLoginPage() throws Exception {
    request()
        .dontFollowRedirect()
        .get("/572/auth/form")
        .expect(302)
        .header("Location", "/572/login");
  }

  @Test
  public void loginPage() throws Exception {
    request()
        .get("/572/auth/form")
        .expect(rsp -> {
          Document html = Jsoup.parse(rsp);
          String action = (html.select("form").attr("action"));
          assertEquals("/auth?client_name=FormClient", action);
        });
  }

}
