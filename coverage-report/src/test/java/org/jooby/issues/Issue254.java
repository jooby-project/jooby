package org.jooby.issues;

import org.jooby.Results;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue254 extends ServerFeature {

  {
    get("/new-session", req -> {
      req.session();
      return Results.ok();
    });

    get("/present", req -> {
      return req.ifSession().isPresent();
    });

    get("/destroy", req -> {
      req.ifSession().ifPresent(session -> session.destroy());
      return Results.ok();
    });
  }

  @Test
  public void sessionOnceCreatedShouldBeAvailableBetweenRequests() throws Exception {
    request().get("/new-session")
        .expect(200);

    request().get("/present")
        .expect("true");

    request().get("/present")
        .expect("true");

    request().get("/destroy")
        .expect(200);

    request().get("/present")
        .expect("false");
  }

}
