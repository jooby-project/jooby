package org.jooby.pac4j2;

import org.jooby.test.ServerFeature;
import org.junit.Test;
import org.pac4j.core.profile.CommonProfile;

import static org.junit.Assert.assertEquals;

public class AuthWithPathPatternFeature extends ServerFeature {

  {

    use(new Auth().basic("/private/**"));

    get("/hello", () -> "hi");

    get("/private", req -> {
      CommonProfile p1 = req.require(CommonProfile.class);
      CommonProfile p2 = req.require(CommonProfile.class);
      assertEquals(p1, p2);
      return p1.getId();
    });
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
