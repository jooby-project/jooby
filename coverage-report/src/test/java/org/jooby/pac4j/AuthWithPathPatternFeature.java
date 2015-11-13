package org.jooby.pac4j;

import static org.junit.Assert.assertEquals;

import org.jooby.test.ServerFeature;
import org.junit.Test;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.http.profile.HttpProfile;

public class AuthWithPathPatternFeature extends ServerFeature {

  {

    use(new Auth().basic("/private/**"));

    get("/hello", () -> "hi");

    get("/private", req -> {
      UserProfile p1 = req.require(UserProfile.class);
      HttpProfile p2 = req.require(HttpProfile.class);
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
