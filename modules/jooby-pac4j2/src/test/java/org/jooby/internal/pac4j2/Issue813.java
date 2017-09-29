package org.jooby.internal.pac4j2;

import org.junit.Test;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.oidc.profile.OidcProfile;

import static org.junit.Assert.assertEquals;

public class Issue813 {

  @Test
  public void shouldNotThrowClassCastExceptionWithOidc() {
    assertEquals(OidcProfile.class, ClientType.typeOf(new OidcClient<OidcProfile>(new OidcConfiguration()).getClass()));
  }
}
