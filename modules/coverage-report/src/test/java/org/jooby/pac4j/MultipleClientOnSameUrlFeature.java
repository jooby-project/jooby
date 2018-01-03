package org.jooby.pac4j;

import com.google.common.io.BaseEncoding;
import org.jooby.test.ServerFeature;
import org.junit.Test;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.TokenCredentials;
import org.pac4j.core.credentials.authenticator.Authenticator;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.core.profile.creator.AuthenticatorProfileCreator;
import org.pac4j.http.client.direct.DirectBasicAuthClient;
import org.pac4j.http.client.direct.HeaderClient;
import org.pac4j.http.credentials.authenticator.test.SimpleTestUsernamePasswordAuthenticator;

import java.util.Optional;

@SuppressWarnings("rawtypes")
public class MultipleClientOnSameUrlFeature extends ServerFeature {

  public static class HeaderAuthenticator implements Authenticator<TokenCredentials>  {

    @Override
    public void validate(final TokenCredentials credentials, final WebContext context) throws CredentialsException {
      if (credentials == null || !credentials.getToken().equals("1234")) {
        throw new CredentialsException("Bad token");
      }
    }

  }

  {

    HeaderClient client = new HeaderClient();
    client.setHeaderName("X-Token");
    client.setAuthenticator(new HeaderAuthenticator());
    client.setProfileCreator((credentials, ctx) -> {
      CommonProfile profile = new CommonProfile();
      profile.setId(credentials.getToken());
      return profile;
    });
    use(new Auth()
        .client(client)
        .client(new DirectBasicAuthClient(
            new SimpleTestUsernamePasswordAuthenticator(),
            new AuthenticatorProfileCreator<>())));

    get("/multi-client", req -> {
      Optional<CommonProfile> op = req.require(ProfileManager.class).get(true);
      return op.map(p -> p.getClientName()).orElse("not logged in");
    });
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
