package org.jooby.issues;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import org.jooby.pac4j.Auth;
import org.jooby.test.ServerFeature;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;

import static org.junit.Assert.*;

public class Issue624c extends ServerFeature {

  {
    use(ConfigFactory.empty()
        .withValue("application.path", ConfigValueFactory.fromAnyRef("/624")));

    use(new Auth());

    get("/saved-url", req -> req.path());
  }

  @Test
  public void shouldForceARedirect() throws Exception {
    request()
        .get("/624/saved-url")
        .expect(rsp -> {
          Document html = Jsoup.parse(rsp);
          String action = (html.select("form").attr("action"));
          assertEquals("/624/auth?client_name=FormClient", action);
        });

    request()
        .get("/624/auth?username=test&password=test")
        .expect("/saved-url");
  }

}
