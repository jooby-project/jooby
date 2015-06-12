package org.jooby.pac4j;

import org.jooby.test.ServerFeature;
import org.junit.Test;
import org.pac4j.http.profile.HttpProfile;

public class AuthWithPathPatternFeature extends ServerFeature {

  {

    use(new Auth().basic("/private/**"));

    get("/hello", () -> "hi");

    get("/private", req -> req.require(HttpProfile.class).getId());
  }

  @Test
  public void noauth() throws Exception {
    request()
        .get("/hello")
        .expect("hi");
  }

  @Test
  public void auth() throws Exception {
    request()
        .basic("test", "test")
        .get("/private")
        .expect("test");
  }

}
