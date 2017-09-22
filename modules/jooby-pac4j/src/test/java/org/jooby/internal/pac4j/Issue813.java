package org.jooby.internal.pac4j;

import org.junit.Test;
import org.pac4j.core.client.BaseClient;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.http.client.indirect.FormClient;
import org.pac4j.oauth.client.FacebookClient;
import org.pac4j.oauth.profile.facebook.FacebookProfile;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.oidc.profile.OidcProfile;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.profile.SAML2Profile;

import static org.junit.Assert.assertEquals;

public class Issue813 {

  @Test
  public void shouldNotThrowClassCastExceptionWithOidc() {
    assertEquals(OidcClient.class, ClientType.typeOf(new OidcClient<OidcProfile>(new OidcConfiguration()).getClass()));
  }
}
