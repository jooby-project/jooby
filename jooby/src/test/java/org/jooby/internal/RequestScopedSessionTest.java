package org.jooby.internal;

import static org.easymock.EasyMock.expect;
import org.jooby.Cookie;
import org.jooby.Response;
import org.jooby.Session;
import org.jooby.test.MockUnit;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * TODO: complete unit tests.
 */
public class RequestScopedSessionTest {

  private MockUnit.Block destroy = unit -> {
    SessionImpl session = unit.get(SessionImpl.class);
    session.destroy();
  };

  private MockUnit.Block resetSession = unit -> {
    unit.get(Runnable.class).run();
  };

  private MockUnit.Block cookie = unit -> {
    Cookie.Definition cookie = unit.mock(Cookie.Definition.class);
    expect(unit.get(SessionManager.class).cookie()).andReturn(cookie);

    expect(cookie.maxAge(0)).andReturn(cookie);

    Response rsp = unit.get(Response.class);
    expect(rsp.cookie(cookie)).andReturn(rsp);
  };

  private MockUnit.Block smDestroy = unit -> {
    unit.get(SessionManager.class).destroy(unit.get(SessionImpl.class));
  };

  @Test
  public void shouldDestroySession() throws Exception {
    new MockUnit(SessionManager.class, Response.class, SessionImpl.class, Runnable.class)
        .expect(sid("sid"))
        .expect(destroy)
        .expect(resetSession)
        .expect(smDestroy)
        .expect(cookie)
        .run(unit -> {
          RequestScopedSession session = new RequestScopedSession(
              unit.get(SessionManager.class), unit.get(Response.class), unit.get(SessionImpl.class),
              unit.get(Runnable.class));
          session.destroy();
          // NOOP
          session.destroy();
        });
  }

  @Test(expected = Session.Destroyed.class)
  public void destroyedSession() throws Exception {
    new MockUnit(SessionManager.class, Response.class, SessionImpl.class, Runnable.class)
        .expect(sid("sid"))
        .expect(destroy)
        .expect(resetSession)
        .expect(smDestroy)
        .expect(cookie)
        .run(unit -> {
          RequestScopedSession session = new RequestScopedSession(
              unit.get(SessionManager.class), unit.get(Response.class), unit.get(SessionImpl.class),
              unit.get(Runnable.class));
          session.destroy();
          session.id();
        });
  }

  @Test
  public void isDestroyed() throws Exception {
    new MockUnit(SessionManager.class, Response.class, SessionImpl.class, Runnable.class)
        .expect(sid("sid"))
        .expect(destroy)
        .expect(resetSession)
        .expect(smDestroy)
        .expect(cookie)
        .expect(isDestroyed(false))
        .run(unit -> {
          RequestScopedSession session = new RequestScopedSession(
              unit.get(SessionManager.class), unit.get(Response.class), unit.get(SessionImpl.class),
              unit.get(Runnable.class));
          assertEquals(false, session.isDestroyed());
          session.destroy();
          assertEquals(true, session.isDestroyed());
        });
  }

  private MockUnit.Block isDestroyed(boolean destroyed) {
    return unit -> {
      expect(unit.get(SessionImpl.class).isDestroyed()).andReturn(destroyed);
    };
  }

  private MockUnit.Block sid(String sid) {
    return unit -> {
      SessionImpl session = unit.get(SessionImpl.class);
      expect(session.id()).andReturn(sid);
    };
  }
}
