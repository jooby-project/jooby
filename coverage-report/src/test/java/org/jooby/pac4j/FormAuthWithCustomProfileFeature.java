package org.jooby.pac4j;

import org.jooby.test.ServerFeature;
import org.junit.Test;
import org.pac4j.http.credentials.SimpleTestUsernamePasswordAuthenticator;
import org.pac4j.http.profile.HttpProfile;
import org.pac4j.http.profile.UsernameProfileCreator;

public class FormAuthWithCustomProfileFeature extends ServerFeature {

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

    use(new Auth().form("*", SimpleTestUsernamePasswordAuthenticator.class, MyProfileCreator.class));

    get("/", req -> req.require(HttpProfile.class).getId());
  }

  @Test
  public void auth() throws Exception {
    request()
        .get("/auth?username=test&password=test")
        .expect("test");
  }

}
