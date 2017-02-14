package org.jooby.issues;

import org.jooby.pac4j.Auth;
import org.jooby.test.ServerFeature;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;

import static org.junit.Assert.*;

public class Issue624d extends ServerFeature {

  {
    use(new Auth());

    get("/saved-url", req -> req.path());
  }

  @Test
  public void shouldForceARedirect() throws Exception {
    request()
        .get("/saved-url")
        .expect(rsp -> {
          Document html = Jsoup.parse(rsp);
          String action = (html.select("form").attr("action"));
          assertEquals("/auth?client_name=FormClient", action);
        });

    request()
        .get("/auth?username=test&password=test")
        .expect("/saved-url");
  }

}
