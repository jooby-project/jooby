package org.jooby.issues;

import org.jooby.Results;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue469b extends ServerFeature {

  {
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
        .get("/redirect")
        .execute()
        .header("Location", "/");

    request()
        .dontFollowRedirect()
        .get("/credirect")
        .execute()
        .header("Location", "/");

    request()
        .dontFollowRedirect()
        .get("/redirect?p=http://google.com")
        .execute()
        .header("Location", "http://google.com");
  }

}
