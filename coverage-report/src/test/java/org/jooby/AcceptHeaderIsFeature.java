package org.jooby;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class AcceptHeaderIsFeature extends ServerFeature {

  {
    get("/accept/html", req -> req.is("html"));

    get("/accept/json", req -> req.is(MediaType.json));

    get("/accept/any", req -> req.is(MediaType.ALL));
  }

  @Test
  public void html() throws Exception {
    request()
        .get("/accept/html")
        .header("Accept", "text/html")
        .expect("true");

    request()
        .get("/accept/html")
        .header("Accept", "text/plain")
        .expect("false");
  }

  @Test
  public void json() throws Exception {
    request()
        .get("/accept/json")
        .header("Accept", "application/json")
        .expect("true");

  }

  @Test
  public void any() throws Exception {
    request()
        .get("/accept/any")
        .header("Accept", "application/json")
        .expect("true");

    request()
        .get("/accept/any")
        .header("Accept", "text/html")
        .expect("true");

  }

}
