package org.jooby.issues;

import org.jooby.Results;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class Issue469 extends ServerFeature {

  {
    use(ConfigFactory.empty()
        .withValue("application.path", ConfigValueFactory.fromAnyRef("/469")));

    get("/", req -> "OK");

    get("/redirect", req -> {
      return Results.redirect(req.param("p").value("/"));
    });

    get("/credirect", req -> {
      return Results.redirect(req.contextPath() + req.param("p").value("/"));
    });
  }

  @Test
  public void redirectOptions() throws Exception {
    request()
        .dontFollowRedirect()
        .get("/469/redirect")
        .execute()
        .header("Location", "/469/");

    request()
        .dontFollowRedirect()
        .get("/469/credirect")
        .execute()
        .header("Location", "/469/");

    request()
        .dontFollowRedirect()
        .get("/469/redirect?p=http://google.com")
        .execute()
        .header("Location", "http://google.com");
  }

}
