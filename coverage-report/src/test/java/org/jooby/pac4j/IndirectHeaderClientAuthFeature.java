package org.jooby.pac4j;

import javax.inject.Inject;

import org.jooby.test.ServerFeature;
import org.junit.Test;
import org.pac4j.core.client.BaseClient;
import org.pac4j.core.client.Mechanism;
import org.pac4j.core.credentials.Authenticator;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.http.client.AbstractHeaderClient;
import org.pac4j.http.credentials.TokenCredentials;
import org.pac4j.http.profile.HttpProfile;

public class CustomHeaderAuthFeature extends ServerFeature {

  public static class HeaderAuthenticator implements Authenticator<TokenCredentials> {

    @Override
    public void validate(final TokenCredentials credentials) {
      if (!credentials.getToken().equals("1234")) {
        throw new CredentialsException("Bad token");
      }
    }

  }

  public static class HeaderClient extends AbstractHeaderClient<TokenCredentials> {

    @Inject
    public HeaderClient(final HeaderAuthenticator auth) {
      setRealmName("authentication required");
      setHeaderName("X-Token");
      setPrefixHeader("");
      setAuthenticator(auth);
      setProfileCreator(credentials -> {
        HttpProfile profile = new HttpProfile();
        profile.setId(credentials.getToken());
        return profile;
      });
    }

    @Override
    protected void internalInit() {
    }

    @Override
    protected TokenCredentials retrieveCredentialsFromHeader(final String header) {
      return new TokenCredentials(header, "token");
    }

    @Override
    protected BaseClient<TokenCredentials, HttpProfile> newClient() {
      HeaderClient client = new HeaderClient((HeaderAuthenticator) getAuthenticator());
      return client;
    }

    @Override
    public Mechanism getMechanism() {
      return Mechanism.BASICAUTH_MECHANISM;
    }

  }

  {

    use(new Auth().client(HeaderClient.class));

    get("/auth/header", req -> req.path());
  }

  @Test
  public void auth() throws Exception {
    request()
        .get("/auth/header")
        .header("X-Token", "1234")
        .expect("/auth/header");
  }

  @Test
  public void unauthorizedAjax() throws Exception {
    request()
        .get("/auth/header")
        .header("X-Requested-With", "XMLHttpRequest")
        .expect(401);
  }

  @Test
  public void unauthorized() throws Exception {
    request()
        .get("/auth/header")
        .expect(401);

  }

  @Test
  public void badCredentials() throws Exception {
    request()
        .get("/auth/header")
        .header("X-Token", "123")
        .expect(401);
  }

}
