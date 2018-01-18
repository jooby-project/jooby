package org.jooby.internal.pac4j2;

import com.google.common.collect.ImmutableList;
import static org.easymock.EasyMock.expect;
import org.jooby.Registry;
import org.jooby.test.MockUnit;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.pac4j.core.authorization.authorizer.Authorizer;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.profile.CommonProfile;

public class Pac4jAuthorizerTest {

  @Test
  public void shouldForwardIsAuthorizedToAuthorizer() throws Exception {
    new MockUnit(Authorizer.class, WebContext.class, CommonProfile.class, Registry.class)
        .expect(unit -> {
          Authorizer authorizer = unit.get(Authorizer.class);
          expect(authorizer.isAuthorized(unit.get(WebContext.class),
              ImmutableList.of(unit.get(CommonProfile.class)))).andReturn(true);

          Registry registry = unit.get(Registry.class);
          expect(registry.require(Authorizer.class)).andReturn(authorizer);
        })
        .run(unit -> {
          Pac4jAuthorizer authorizer = new Pac4jAuthorizer(Authorizer.class)
              .setRegistry(unit.get(Registry.class));
          assertEquals(true,
              authorizer.isAuthorized(unit.get(WebContext.class), ImmutableList.of(unit.get(
                  CommonProfile.class))));
        });
  }
}
