package org.jooby.pac4j;

import org.jooby.test.ServerFeature;
import org.junit.Test;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.http.client.direct.DirectBasicAuthClient;
import org.pac4j.http.client.direct.HeaderClient;
import org.pac4j.http.credentials.HttpCredentials;
import org.pac4j.http.credentials.TokenCredentials;
import org.pac4j.http.credentials.authenticator.Authenticator;
import org.pac4j.http.credentials.authenticator.test.SimpleTestUsernamePasswordAuthenticator;
import org.pac4j.http.profile.HttpProfile;
import org.pac4j.http.profile.creator.AuthenticatorProfileCreator;

import com.google.common.io.BaseEncoding;

public class MultipleClientOnSameUrlFeature extends ServerFeature {

  public static class HeaderAuthenticator implements Authenticator<TokenCredentials> {

    @Override
    public void validate(final TokenCredentials credentials) {
      if (credentials == null || !credentials.getToken().equals("1234")) {
        throw new CredentialsException("Bad token");
      }
    }

  }

  {

    HeaderClient client = new HeaderClient();
    client.setHeaderName("X-Token");
    client.setAuthenticator(new HeaderAuthenticator());
    client.setProfileCreator(credentials -> {
      HttpProfile profile = new HttpProfile();
      profile.setId(credentials.getToken());
      return profile;
    });
    use(new Auth()
        .client("/multi-client/**", client)
        .client("/multi-client/**", new DirectBasicAuthClient(
            new SimpleTestUsernamePasswordAuthenticator(),
            new AuthenticatorProfileCreator<HttpCredentials, UserProfile>())));

    get("/multi-client", req -> req.get(Auth.CNAME));
  }

  @Test
  public void auth() throws Exception {
    request()
        .get("/multi-client")
        .header("X-Token", "1234")
        .expect("HeaderClient")
        .expect(200);
  }

  @Test
  public void basic() throws Exception {
    request()
        .get("/multi-client")
        .header("Authorization", "Basic " + BaseEncoding.base64().encode("test:test".getBytes()))
        .expect("DirectBasicAuthClient")
        .expect(200);
  }

  @Test
  public void unauthorized() throws Exception {
    request()
        .get("/multi-client")
        .expect(401);
  }

}
