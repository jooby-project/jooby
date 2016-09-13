package org.jooby.internal.pac4j;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.pac4j.core.client.BaseClient;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.http.client.indirect.FormClient;
import org.pac4j.oauth.client.FacebookClient;
import org.pac4j.oauth.profile.facebook.FacebookProfile;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.profile.SAML2Profile;

public class ClientTypeTest {

  @Test
  public void defaults() {
    new ClientType();
  }

  @Test
  public void extractType() {
    assertEquals(CommonProfile.class, ClientType.typeOf(BaseClient.class));
    assertEquals(FacebookProfile.class, ClientType.typeOf(FacebookClient.class));
    assertEquals(SAML2Profile.class, ClientType.typeOf(SAML2Client.class));
    assertEquals(CommonProfile.class, ClientType.typeOf(FormClient.class));
  }
}
