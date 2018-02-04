package org.jooby.internal.pac4j2;

import com.google.common.collect.ImmutableList;
import static org.easymock.EasyMock.expect;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.Session;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;

import java.util.Optional;

public class Pac4jGrantAccessAdapterTest {

  @Test
  public void shouldAllowAccess() throws Exception {
    new MockUnit(Request.class, Response.class, Route.Chain.class, ProfileManager.class,
        WebContext.class, Session.class, CommonProfile.class)
        .expect(unit -> {
          CommonProfile profile = unit.get(CommonProfile.class);

          ProfileManager pm = unit.get(ProfileManager.class);
          expect(pm.getAll(true)).andReturn(ImmutableList.of(profile));

          Request req = unit.get(Request.class);
          expect(req.require(ProfileManager.class)).andReturn(pm);
          expect(req.ifSession()).andReturn(Optional.of(unit.get(Session.class)));

          Pac4jClientType.profileTypes(profile.getClass(),
              type -> expect(req.set(type, profile)).andReturn(req));

          Response rsp = unit.get(Response.class);
          unit.get(Route.Chain.class).next(req, rsp);
        })
        .run(unit -> {
          new Pac4jGrantAccessAdapter(unit.get(Request.class), unit.get(Response.class),
              unit.get(Route.Chain.class))
              .adapt(unit.get(WebContext.class));
        });
  }
}
