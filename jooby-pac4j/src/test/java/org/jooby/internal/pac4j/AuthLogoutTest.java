package org.jooby.internal.pac4j;

import static org.easymock.EasyMock.expect;

import java.util.Optional;

import org.jooby.Mutant;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Session;
import org.jooby.pac4j.Auth;
import org.jooby.pac4j.AuthStore;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.pac4j.core.profile.UserProfile;

public class AuthLogoutTest {

  @SuppressWarnings("rawtypes")
  @Test
  public void unset() throws Exception {
    new MockUnit(Request.class, Response.class, Session.class, AuthStore.class, UserProfile.class)
        .expect(unit -> {
          Session session = unit.get(Session.class);

          Request req = unit.get(Request.class);
          expect(req.ifSession()).andReturn(Optional.of(session));
          expect(req.require(AuthStore.class)).andReturn(unit.get(AuthStore.class));
          expect(req.ifGet("auth.logout.redirectTo")).andReturn(Optional.empty());
        })
        .expect(unit -> {
          Optional<String> id = Optional.of("1");

          UserProfile profile = unit.get(UserProfile.class);

          AuthStore store = unit.get(AuthStore.class);
          expect(store.unset("1")).andReturn(Optional.of(profile));

          Mutant attr = unit.mock(Mutant.class);
          expect(attr.toOptional()).andReturn(id);

          Session session = unit.get(Session.class);
          expect(session.unset(Auth.ID)).andReturn(attr);
        })
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          rsp.redirect("/");
        })
        .run(unit -> {
          new AuthLogout("/")
              .handle(unit.get(Request.class), unit.get(Response.class));
        });
  }

  @Test
  public void unsetIgnore() throws Exception {
    new MockUnit(Request.class, Response.class, Session.class)
        .expect(unit -> {
          Session session = unit.get(Session.class);

          Request req = unit.get(Request.class);
          expect(req.ifSession()).andReturn(Optional.of(session));
          expect(req.ifGet("auth.logout.redirectTo")).andReturn(Optional.empty());
        })
        .expect(unit -> {
          Optional<String> id = Optional.empty();

          Mutant attr = unit.mock(Mutant.class);
          expect(attr.toOptional()).andReturn(id);

          Session session = unit.get(Session.class);
          expect(session.unset(Auth.ID)).andReturn(attr);
        })
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          rsp.redirect("/");
        })
        .run(unit -> {
          new AuthLogout("/")
              .handle(unit.get(Request.class), unit.get(Response.class));
        });
  }

  @Test
  public void unsetNoSession() throws Exception {
    new MockUnit(Request.class, Response.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.ifSession()).andReturn(Optional.empty());
          expect(req.ifGet("auth.logout.redirectTo")).andReturn(Optional.empty());
        })
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          rsp.redirect("/");
        })
        .run(unit -> {
          new AuthLogout("/")
              .handle(unit.get(Request.class), unit.get(Response.class));
        });
  }

}
