package org.jooby.pac4j;

import org.jooby.test.ServerFeature;
import org.junit.Test;
import org.pac4j.http.credentials.SimpleTestUsernamePasswordAuthenticator;
import org.pac4j.http.profile.HttpProfile;
import org.pac4j.http.profile.UsernameProfileCreator;

public class BasicAuthWithCustomProfileFeature extends ServerFeature {

  @SuppressWarnings("serial")
  public static class MyProfile extends HttpProfile {

  }

  public static class MyProfileCreator extends UsernameProfileCreator {
    @Override
    public HttpProfile create(
        final org.pac4j.http.credentials.UsernamePasswordCredentials credentials) {
      MyProfile profile = new MyProfile();
      profile.setId("test");
      return profile;
    }
  }

  {

    use(new Auth()
        .basic("*", SimpleTestUsernamePasswordAuthenticator.class, MyProfileCreator.class));

    get("/auth/basic", req -> req.require(HttpProfile.class).getId());
  }

  @Test
  public void auth() throws Exception {
    request()
        .basic("test", "test")
        .get("/auth/basic")
        .expect("test");
  }

}
