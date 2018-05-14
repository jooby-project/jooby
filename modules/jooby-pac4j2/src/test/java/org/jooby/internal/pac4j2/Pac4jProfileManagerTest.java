package org.jooby.internal.pac4j2;

import static org.easymock.EasyMock.expect;
import org.jooby.test.MockUnit;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.profile.ProfileManager;

import java.util.function.Function;

public class Pac4jProfileManagerTest {
  @Test
  public void shouldCreateProfileManager() throws Exception {
    new MockUnit(WebContext.class, Config.class, ProfileManager.class)
        .expect(unit -> {
          Function<WebContext, ProfileManager> pmf = unit.mock(Function.class);
          expect(pmf.apply(unit.get(WebContext.class))).andReturn(unit.get(ProfileManager.class));

          Config config = unit.get(Config.class);
          expect(config.getProfileManagerFactory()).andReturn(pmf);
        })
        .run(unit -> {
          Pac4jProfileManager pmp = new Pac4jProfileManager(unit.get(Config.class),
              unit.get(WebContext.class));
          assertEquals(unit.get(ProfileManager.class), pmp.get());
        });
  }
}
