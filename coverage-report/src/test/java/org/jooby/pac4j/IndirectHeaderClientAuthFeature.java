package org.jooby.pac4j;

import javax.inject.Inject;

import org.jooby.test.ServerFeature;
import org.junit.Test;
import org.pac4j.core.client.BaseClient;
import org.pac4j.core.client.ClientType;
import org.pac4j.core.client.RedirectAction;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.core.exception.RequiresHttpAction;
import org.pac4j.http.client.indirect.IndirectHttpClient;
import org.pac4j.http.credentials.TokenCredentials;
import org.pac4j.http.credentials.authenticator.Authenticator;
import org.pac4j.http.credentials.extractor.HeaderExtractor;
import org.pac4j.http.profile.HttpProfile;

public class IndirectHeaderClientAuthFeature extends ServerFeature {

  public static class HeaderAuthenticator implements Authenticator<TokenCredentials> {

    @Override
    public void validate(final TokenCredentials credentials) {
      if (credentials == null || !credentials.getToken().equals("1234")) {
        throw new CredentialsException("Bad token");
      }
    }

  }

  public static class HeaderClient extends IndirectHttpClient<TokenCredentials> {

    @Inject
    public HeaderClient(final HeaderAuthenticator auth) {
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
    protected BaseClient<TokenCredentials, HttpProfile> newClient() {
      HeaderClient client = new HeaderClient((HeaderAuthenticator) getAuthenticator());
      return client;
    }

    @Override
    protected boolean isDirectRedirection() {
      return true;
    }

    @Override
    protected RedirectAction retrieveRedirectAction(final WebContext context) {
      return RedirectAction.redirect(getContextualCallbackUrl(context));
    }

    @Override
    protected TokenCredentials retrieveCredentials(final WebContext context)
        throws RequiresHttpAction {
      try {
        TokenCredentials credentials = new HeaderExtractor("X-Token", "", "token").extract(context);
        getAuthenticator().validate(credentials);
        return credentials;
      } catch (final CredentialsException e) {
        logger.error("Credentials retrieval / validation failed", e);
        throw RequiresHttpAction.unauthorized("Requires authentication", context,
            this.getName());
      }
    }

    @Override
    public ClientType getClientType() {
      return ClientType.HEADER_BASED;
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
