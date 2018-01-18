package org.jooby.internal.pac4j2;

import com.google.common.collect.Sets;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.http.client.direct.HeaderClient;
import org.pac4j.http.client.indirect.FormClient;
import org.pac4j.oauth.client.FacebookClient;
import org.pac4j.oauth.profile.facebook.FacebookProfile;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.profile.OidcProfile;

import java.util.Set;

public class Pac4jClientTypeTest {

  @Test
  public void constructorDoesNothing() {
    new Pac4jClientType();
  }

  @Test
  public void shouldGetProfileTypeFromClientType() {
    assertEquals(OidcProfile.class, Pac4jClientType.clientType(OidcClient.class));
    assertEquals(FacebookProfile.class, Pac4jClientType.clientType(FacebookClient.class));
    assertEquals(CommonProfile.class, Pac4jClientType.clientType(FormClient.class));
    assertEquals(CommonProfile.class, Pac4jClientType.clientType(HeaderClient.class));
  }

  @Test
  public void shouldGetAllProfiles() {
    profileTypes(OidcProfile.class, CommonProfile.class, UserProfile.class);
    profileTypes(FacebookProfile.class, CommonProfile.class, UserProfile.class);
    profileTypes(CommonProfile.class, CommonProfile.class, UserProfile.class);
    profileTypes(UserProfile.class, UserProfile.class);
  }

  private void profileTypes(Class<?> baseType, Class<?>... expectedTypes) {
    Set<Class> result = Sets.newHashSet(expectedTypes);
    result.add(baseType);
    Pac4jClientType.profileTypes(baseType, result::remove);
    assertEquals("Found " + result, 0, result.size());
  }
}
