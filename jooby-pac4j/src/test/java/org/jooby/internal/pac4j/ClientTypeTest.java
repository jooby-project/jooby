package org.jooby.internal.pac4j;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.pac4j.core.client.BaseClient;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.http.client.FormClient;
import org.pac4j.oauth.client.FacebookClient;
import org.pac4j.oauth.profile.facebook.FacebookProfile;
import org.pac4j.saml.client.Saml2Client;
import org.pac4j.saml.profile.Saml2Profile;

public class ClientTypeTest {

  @Test
  public void defaults() {
    new ClientType();
  }

  @Test
  public void extractType() {
    assertEquals(UserProfile.class, ClientType.typeOf(BaseClient.class));
    assertEquals(FacebookProfile.class, ClientType.typeOf(FacebookClient.class));
    assertEquals(Saml2Profile.class, ClientType.typeOf(Saml2Client.class));
    assertEquals(UserProfile.class, ClientType.typeOf(FormClient.class));
  }
}
