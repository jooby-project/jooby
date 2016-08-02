package org.jooby.internal;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Optional;

import org.jooby.Cookie;
import org.jooby.Mutant;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Result;
import org.jooby.Route;
import org.jooby.Route.After;
import org.jooby.Session;
import org.jooby.internal.parser.ParserExecutor;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CookieSessionManager.class, SessionImpl.class, Cookie.class })
public class CookieSessionManagerTest {

  private Block cookie = unit -> {
    Session.Definition sdef = unit.get(Session.Definition.class);
    expect(sdef.cookie()).andReturn(unit.get(Cookie.Definition.class));
  };

  private Block push = unit -> {
    Response rsp = unit.get(Response.class);
    rsp.after(unit.capture(Route.After.class));
  };

  @Test
  public void newInstance() throws Exception {
    String secret = "shhh";
    new MockUnit(Session.Definition.class, ParserExecutor.class, Cookie.Definition.class)
        .expect(cookie)
        .expect(maxAge(-1))
        .run(unit -> {
          new CookieSessionManager(unit.get(ParserExecutor.class),
              unit.get(Session.Definition.class), secret);
        });
  }

  @Test
  public void destroy() throws Exception {
    String secret = "shhh";
    new MockUnit(Session.Definition.class, ParserExecutor.class, Cookie.Definition.class,
        Session.class)
            .expect(cookie)
            .expect(maxAge(-1))
            .run(unit -> {
              new CookieSessionManager(unit.get(ParserExecutor.class),
                  unit.get(Session.Definition.class), secret)
                      .destroy(unit.get(Session.class));
            });
  }

  @Test
  public void requestDone() throws Exception {
    String secret = "shhh";
    new MockUnit(Session.Definition.class, ParserExecutor.class, Cookie.Definition.class,
        Session.class)
            .expect(cookie)
            .expect(maxAge(-1))
            .run(unit -> {
              new CookieSessionManager(unit.get(ParserExecutor.class),
                  unit.get(Session.Definition.class), secret)
                      .requestDone(unit.get(Session.class));
            });
  }

  @Test
  public void create() throws Exception {
    String secret = "shhh";
    new MockUnit(Session.Definition.class, ParserExecutor.class, Cookie.Definition.class,
        Request.class, Response.class, SessionImpl.class)
            .expect(cookie)
            .expect(maxAge(-1))
            .expect(sessionBuilder(Session.COOKIE_SESSION, true, -1))
            .expect(push)
            .run(unit -> {
              Session session = new CookieSessionManager(unit.get(ParserExecutor.class),
                  unit.get(Session.Definition.class), secret)
                      .create(unit.get(Request.class), unit.get(Response.class));
              assertEquals(unit.get(SessionImpl.class), session);
            });
  }

  @Test
  public void saveAfter() throws Exception {
    String secret = "shhh";
    String signed = "$#!";
    new MockUnit(Session.Definition.class, ParserExecutor.class, Cookie.Definition.class,
        Request.class, Response.class, SessionImpl.class)
            .expect(cookie)
            .expect(maxAge(-1))
            .expect(sessionBuilder(Session.COOKIE_SESSION, true, -1))
            .expect(push)
            .expect(unit -> {
              Cookie.Definition cookie = unit.get(Cookie.Definition.class);
              expect(cookie.name()).andReturn(Optional.of("sid"));

              Mutant mutant = unit.mock(Mutant.class);
              expect(mutant.toOptional()).andReturn(Optional.of(signed));

              Request req = unit.get(Request.class);
              expect(req.cookie("sid")).andReturn(mutant);
            })
            .expect(unit -> {
              SessionImpl session = unit.get(SessionImpl.class);

              expect(session.attributes()).andReturn(ImmutableMap.of("foo", "2"));

              Request req = unit.get(Request.class);
              expect(req.session()).andReturn(session);
            })
            .expect(unit -> {
              unit.mockStatic(Cookie.Signature.class);
              expect(Cookie.Signature.unsign(signed, secret)).andReturn("foo=1");
            })
            .expect(signCookie(secret, "foo=2", "sss"))
            .expect(sendCookie())
            .run(unit -> {
              Session session = new CookieSessionManager(unit.get(ParserExecutor.class),
                  unit.get(Session.Definition.class), secret)
                      .create(unit.get(Request.class), unit.get(Response.class));
              assertEquals(unit.get(SessionImpl.class), session);
            }, unit -> {
              After next = unit.captured(Route.After.class).iterator().next();
              Result ok = next.handle(unit.get(Request.class), unit.get(Response.class),
                  org.jooby.Results.ok());
              assertNotNull(ok);
            });
  }

  @Test
  public void saveAfterTouchSession() throws Exception {
    String secret = "shhh";
    String signed = "$#!";
    new MockUnit(Session.Definition.class, ParserExecutor.class, Cookie.Definition.class,
        Request.class, Response.class, SessionImpl.class)
            .expect(cookie)
            .expect(maxAge(30))
            .expect(sessionBuilder(Session.COOKIE_SESSION, true, -1))
            .expect(push)
            .expect(unit -> {
              Cookie.Definition cookie = unit.get(Cookie.Definition.class);
              expect(cookie.name()).andReturn(Optional.of("sid"));

              Mutant mutant = unit.mock(Mutant.class);
              expect(mutant.toOptional()).andReturn(Optional.of(signed));

              Request req = unit.get(Request.class);
              expect(req.cookie("sid")).andReturn(mutant);
            })
            .expect(unit -> {
              SessionImpl session = unit.get(SessionImpl.class);

              expect(session.attributes()).andReturn(ImmutableMap.of("foo", "1"));

              Request req = unit.get(Request.class);
              expect(req.session()).andReturn(session);
            })
            .expect(unit -> {
              unit.mockStatic(Cookie.Signature.class);
              expect(Cookie.Signature.unsign(signed, secret)).andReturn("foo=1");
            })
            .expect(unit -> {
              Cookie.Definition cookie = unit.get(Cookie.Definition.class);
              Cookie.Definition newCookie = unit.constructor(Cookie.Definition.class)
                  .build(cookie);

              expect(newCookie.value(signed)).andReturn(newCookie);
              unit.registerMock(Cookie.Definition.class, newCookie);
            })
            .expect(sendCookie())
            .run(unit -> {
              Session session = new CookieSessionManager(unit.get(ParserExecutor.class),
                  unit.get(Session.Definition.class), secret)
                      .create(unit.get(Request.class), unit.get(Response.class));
              assertEquals(unit.get(SessionImpl.class), session);
            }, unit -> {
              After next = unit.captured(Route.After.class).iterator().next();
              Result ok = next.handle(unit.get(Request.class), unit.get(Response.class),
                  org.jooby.Results.ok());
              assertNotNull(ok);
            });
  }

  private Block sendCookie() {
    return unit -> {
      Cookie.Definition cookie = unit.get(Cookie.Definition.class);
      Response rsp = unit.get(Response.class);
      expect(rsp.cookie(cookie)).andReturn(rsp);
    };
  }

  @Test
  public void noSession() throws Exception {
    String secret = "shh";
    new MockUnit(Config.class, Session.Definition.class, Cookie.Definition.class,
        Session.Store.class, ParserExecutor.class, Request.class, Response.class)
            .expect(cookie)
            .expect(maxAge(-1))
            .expect(unit -> {
              Cookie.Definition cookie = unit.get(Cookie.Definition.class);
              expect(cookie.name()).andReturn(Optional.of("sid"));

              Mutant mutant = unit.mock(Mutant.class);
              expect(mutant.toOptional()).andReturn(Optional.empty());

              Request req = unit.get(Request.class);
              expect(req.cookie("sid")).andReturn(mutant);
            })
            .run(unit -> {
              Session session = new CookieSessionManager(unit.get(ParserExecutor.class),
                  unit.get(Session.Definition.class), secret).get(unit.get(Request.class),
                      unit.get(Response.class));
              assertEquals(null, session);
            });
  }

  @Test
  public void getSession() throws Exception {
    String secret = "shh";
    String signed = "$#!";
    new MockUnit(Config.class, Session.Definition.class, Cookie.Definition.class,
        Session.Store.class, ParserExecutor.class, Request.class, Response.class, SessionImpl.class)
            .expect(cookie)
            .expect(maxAge(-1))
            .expect(unit -> {
              Cookie.Definition cookie = unit.get(Cookie.Definition.class);
              expect(cookie.name()).andReturn(Optional.of("sid"));

              Mutant mutant = unit.mock(Mutant.class);
              expect(mutant.toOptional()).andReturn(Optional.of(signed));

              Request req = unit.get(Request.class);
              expect(req.cookie("sid")).andReturn(mutant);
            })
            .expect(unit -> {
              unit.mockStatic(Cookie.Signature.class);
              expect(Cookie.Signature.unsign(signed, secret)).andReturn("foo=1");
            })
            .expect(sessionBuilder(Session.COOKIE_SESSION, false, -1))
            .expect(unit -> {
              Session.Builder builder = unit.get(Session.Builder.class);
              expect(builder.set(ImmutableMap.of("foo", "1"))).andReturn(builder);
              expect(builder.build()).andReturn(unit.get(SessionImpl.class));
            })
            .expect(push)
            .run(unit -> {
              Session session = new CookieSessionManager(unit.get(ParserExecutor.class),
                  unit.get(Session.Definition.class), secret).get(unit.get(Request.class),
                      unit.get(Response.class));
              assertEquals(unit.get(SessionImpl.class), session);
            });
  }

  private Block signCookie(final String secret, final String value, final String signed) {
    return unit -> {
      unit.mockStatic(Cookie.Signature.class);
      expect(Cookie.Signature.sign(value, secret)).andReturn(signed);

      Cookie.Definition cookie = unit.get(Cookie.Definition.class);
      Cookie.Definition newCookie = unit.constructor(Cookie.Definition.class)
          .build(cookie);

      expect(newCookie.value(signed)).andReturn(newCookie);
      unit.registerMock(Cookie.Definition.class, newCookie);
    };
  }

  private Block maxAge(final Integer maxAge) {
    return unit -> {
      Cookie.Definition session = unit.get(Cookie.Definition.class);
      expect(session.maxAge()).andReturn(Optional.of(maxAge));
    };
  }

  private Block sessionBuilder(final String id, final boolean isNew, final long timeout) {
    return unit -> {
      SessionImpl.Builder builder = unit.constructor(SessionImpl.Builder.class)
          .build(unit.get(ParserExecutor.class), isNew, id, timeout);
      if (isNew) {
        expect(builder.build()).andReturn(unit.get(SessionImpl.class));
      }

      unit.registerMock(Session.Builder.class, builder);
    };
  }

}
