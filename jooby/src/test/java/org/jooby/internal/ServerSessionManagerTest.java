package org.jooby.internal;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.assertEquals;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.jooby.Cookie;
import org.jooby.Mutant;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Session;
import org.jooby.Session.Definition;
import org.jooby.Session.Store;
import org.jooby.internal.parser.ParserExecutor;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.typesafe.config.Config;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ServerSessionManager.class, SessionImpl.class, Cookie.class })
public class ServerSessionManagerTest {

  private Block noSecret = unit -> {
    Config config = unit.get(Config.class);
    expect(config.hasPath("application.secret")).andReturn(false);
  };

  private Block cookie = unit -> {
    Definition session = unit.get(Session.Definition.class);
    expect(session.cookie()).andReturn(unit.get(Cookie.Definition.class));
  };

  private Block storeGet = unit -> {
    Store store = unit.get(Store.class);
    expect(store.get(unit.get(Session.Builder.class)))
        .andReturn(unit.get(SessionImpl.class));
  };

  @Test
  public void newServerSessionManager() throws Exception {
    new MockUnit(Config.class, Session.Definition.class, Cookie.Definition.class,
        Session.Store.class, ParserExecutor.class)
            .expect(noSecret)
            .expect(cookie)
            .expect(saveInterval(-1L))
            .expect(maxAge(-1))
            .run(unit -> {
              new ServerSessionManager(unit.get(Config.class), unit.get(Session.Definition.class),
                  unit.get(Session.Store.class), unit.get(ParserExecutor.class));
            });
  }

  @Test
  public void destroy() throws Exception {
    new MockUnit(Config.class, Session.Definition.class, Cookie.Definition.class,
        Session.Store.class, ParserExecutor.class, Session.class)
            .expect(noSecret)
            .expect(cookie)
            .expect(saveInterval(-1L))
            .expect(maxAge(-1))
            .expect(unit -> {
              Session session = unit.get(Session.class);
              expect(session.id()).andReturn("sid");

              Store store = unit.get(Session.Store.class);
              store.delete("sid");
            })
            .run(unit -> {
              new ServerSessionManager(unit.get(Config.class), unit.get(Session.Definition.class),
                  unit.get(Session.Store.class), unit.get(ParserExecutor.class))
                      .destroy(unit.get(Session.class));
            });
  }

  @Test
  public void storeCreateSession() throws Exception {
    new MockUnit(Config.class, Session.Definition.class, Cookie.Definition.class,
        Session.Store.class, ParserExecutor.class, RequestScopedSession.class, SessionImpl.class)
            .expect(noSecret)
            .expect(cookie)
            .expect(saveInterval(-1L))
            .expect(maxAge(-1))
            .expect(reqSession())
            .expect(unit -> {
              SessionImpl session = unit.get(SessionImpl.class);
              session.touch();
              expect(session.isNew()).andReturn(true);
              session.aboutToSave();

              Store store = unit.get(Store.class);
              store.create(session);

              session.markAsSaved();
            })
            .run(unit -> {
              new ServerSessionManager(unit.get(Config.class), unit.get(Session.Definition.class),
                  unit.get(Session.Store.class), unit.get(ParserExecutor.class))
                      .requestDone(unit.get(RequestScopedSession.class));
            });
  }

  @Test
  public void storeDirtySession() throws Exception {
    new MockUnit(Config.class, Session.Definition.class, Cookie.Definition.class,
        Session.Store.class, ParserExecutor.class, RequestScopedSession.class, SessionImpl.class)
            .expect(noSecret)
            .expect(cookie)
            .expect(saveInterval(-1L))
            .expect(maxAge(-1))
            .expect(reqSession())
            .expect(unit -> {
              SessionImpl session = unit.get(SessionImpl.class);
              session.touch();
              expect(session.isNew()).andReturn(false);
              expect(session.isDirty()).andReturn(true);
              session.aboutToSave();

              Store store = unit.get(Store.class);
              store.save(session);

              session.markAsSaved();
            })
            .run(unit -> {
              new ServerSessionManager(unit.get(Config.class), unit.get(Session.Definition.class),
                  unit.get(Session.Store.class), unit.get(ParserExecutor.class))
                      .requestDone(unit.get(RequestScopedSession.class));
            });
  }

  @Test
  public void storeSaveIntervalSession() throws Exception {
    new MockUnit(Config.class, Session.Definition.class, Cookie.Definition.class,
        Session.Store.class, ParserExecutor.class, RequestScopedSession.class, SessionImpl.class)
            .expect(noSecret)
            .expect(cookie)
            .expect(saveInterval(-1L))
            .expect(maxAge(-1))
            .expect(reqSession())
            .expect(unit -> {
              SessionImpl session = unit.get(SessionImpl.class);
              session.touch();
              expect(session.isNew()).andReturn(false);
              expect(session.isDirty()).andReturn(false);
              expect(session.savedAt()).andReturn(0L);
              session.aboutToSave();

              Store store = unit.get(Store.class);
              store.save(session);

              session.markAsSaved();
            })
            .run(unit -> {
              new ServerSessionManager(unit.get(Config.class), unit.get(Session.Definition.class),
                  unit.get(Session.Store.class), unit.get(ParserExecutor.class))
                      .requestDone(unit.get(RequestScopedSession.class));
            });
  }

  @Test
  public void storeSkipSaveIntervalSession() throws Exception {
    new MockUnit(Config.class, Session.Definition.class, Cookie.Definition.class,
        Session.Store.class, ParserExecutor.class, RequestScopedSession.class, SessionImpl.class)
            .expect(noSecret)
            .expect(cookie)
            .expect(saveInterval(-1L))
            .expect(maxAge(-1))
            .expect(reqSession())
            .expect(unit -> {
              SessionImpl session = unit.get(SessionImpl.class);
              session.touch();
              expect(session.isNew()).andReturn(false);
              expect(session.isDirty()).andReturn(false);
              expect(session.savedAt()).andReturn(Long.MAX_VALUE);
              session.markAsSaved();
            })
            .run(unit -> {
              new ServerSessionManager(unit.get(Config.class), unit.get(Session.Definition.class),
                  unit.get(Session.Store.class), unit.get(ParserExecutor.class))
                      .requestDone(unit.get(RequestScopedSession.class));
            });
  }

  @Test
  public void storeFailure() throws Exception {
    new MockUnit(Config.class, Session.Definition.class, Cookie.Definition.class,
        Session.Store.class, ParserExecutor.class, RequestScopedSession.class, SessionImpl.class)
            .expect(noSecret)
            .expect(cookie)
            .expect(saveInterval(-1L))
            .expect(maxAge(-1))
            .expect(reqSession())
            .expect(unit -> {
              SessionImpl session = unit.get(SessionImpl.class);
              session.touch();
              expect(session.isNew()).andReturn(true);
              session.aboutToSave();
              Store store = unit.get(Store.class);
              store.create(session);
              expectLastCall().andThrow(new IllegalStateException("intentional err"));
            })
            .run(unit -> {
              new ServerSessionManager(unit.get(Config.class), unit.get(Session.Definition.class),
                  unit.get(Session.Store.class), unit.get(ParserExecutor.class))
                      .requestDone(unit.get(RequestScopedSession.class));
            });
  }

  private Block reqSession() {
    return unit -> {
      RequestScopedSession req = unit.get(RequestScopedSession.class);
      expect(req.session()).andReturn(unit.get(SessionImpl.class));
    };
  }

  @Test
  public void noSession() throws Exception {
    new MockUnit(Config.class, Session.Definition.class, Cookie.Definition.class,
        Session.Store.class, ParserExecutor.class, Request.class, Response.class)
            .expect(noSecret)
            .expect(cookie)
            .expect(saveInterval(-1L))
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
              Session session = new ServerSessionManager(unit.get(Config.class),
                  unit.get(Session.Definition.class),
                  unit.get(Session.Store.class), unit.get(ParserExecutor.class))
                      .get(unit.get(Request.class), unit.get(Response.class));
              assertEquals(null, session);
            });
  }

  @Test
  public void getSession() throws Exception {
    String id = "xyz";
    new MockUnit(Config.class, Session.Definition.class, Cookie.Definition.class,
        Session.Store.class, ParserExecutor.class, Request.class, Response.class, SessionImpl.class)
            .expect(noSecret)
            .expect(cookie)
            .expect(saveInterval(-1L))
            .expect(maxAge(-1))
            .expect(unit -> {
              Cookie.Definition cookie = unit.get(Cookie.Definition.class);
              expect(cookie.name()).andReturn(Optional.of("sid"));

              Mutant mutant = unit.mock(Mutant.class);
              expect(mutant.toOptional()).andReturn(Optional.of(id));

              Request req = unit.get(Request.class);
              expect(req.cookie("sid")).andReturn(mutant);
            })
            .expect(sessionBuilder(id, false, -1))
            .expect(storeGet)
            .run(unit -> {
              Session session = new ServerSessionManager(unit.get(Config.class),
                  unit.get(Session.Definition.class),
                  unit.get(Session.Store.class), unit.get(ParserExecutor.class))
                      .get(unit.get(Request.class), unit.get(Response.class));
              assertEquals(unit.get(SessionImpl.class), session);
            });
  }

  @Test
  public void getTouchSessionCookie() throws Exception {
    String id = "xyz";
    new MockUnit(Config.class, Session.Definition.class, Cookie.Definition.class,
        Session.Store.class, ParserExecutor.class, Request.class, Response.class, SessionImpl.class)
            .expect(noSecret)
            .expect(cookie)
            .expect(saveInterval(-1L))
            .expect(maxAge(30))
            .expect(unit -> {
              Cookie.Definition cookie = unit.get(Cookie.Definition.class);
              expect(cookie.name()).andReturn(Optional.of("sid"));

              Mutant mutant = unit.mock(Mutant.class);
              expect(mutant.toOptional()).andReturn(Optional.of(id));

              Request req = unit.get(Request.class);
              expect(req.cookie("sid")).andReturn(mutant);
            })
            .expect(sessionBuilder(id, false, TimeUnit.SECONDS.toMillis(30)))
            .expect(storeGet)
            .expect(unsignedCookie(id))
            .expect(session(id))
            .expect(sendCookie())
            .run(unit -> {
              Session session = new ServerSessionManager(unit.get(Config.class),
                  unit.get(Session.Definition.class),
                  unit.get(Session.Store.class), unit.get(ParserExecutor.class))
                      .get(unit.get(Request.class), unit.get(Response.class));
              assertEquals(unit.get(SessionImpl.class), session);
            });
  }

  @Test
  public void getSignedSession() throws Exception {
    String id = "xyz";
    new MockUnit(Config.class, Session.Definition.class, Cookie.Definition.class,
        Session.Store.class, ParserExecutor.class, Request.class, Response.class, SessionImpl.class)
            .expect(secret("querty"))
            .expect(cookie)
            .expect(saveInterval(-1L))
            .expect(maxAge(-1))
            .expect(unit -> {
              Cookie.Definition cookie = unit.get(Cookie.Definition.class);
              expect(cookie.name()).andReturn(Optional.of("sid"));

              Mutant mutant = unit.mock(Mutant.class);
              expect(mutant.toOptional()).andReturn(Optional.of(id));

              Request req = unit.get(Request.class);
              expect(req.cookie("sid")).andReturn(mutant);
            })
            .expect(unit -> {
              unit.mockStatic(Cookie.Signature.class);
              expect(Cookie.Signature.unsign(id, "querty")).andReturn("unsigned");
            })
            .expect(sessionBuilder("unsigned", false, -1))
            .expect(storeGet)
            .run(unit -> {
              Session session = new ServerSessionManager(unit.get(Config.class),
                  unit.get(Session.Definition.class),
                  unit.get(Session.Store.class), unit.get(ParserExecutor.class))
                      .get(unit.get(Request.class), unit.get(Response.class));
              assertEquals(unit.get(SessionImpl.class), session);
            });
  }

  @Test
  public void createSession() throws Exception {
    new MockUnit(Config.class, Session.Definition.class, Cookie.Definition.class,
        Session.Store.class, ParserExecutor.class, Request.class, Response.class, SessionImpl.class)
            .expect(noSecret)
            .expect(cookie)
            .expect(saveInterval(-1L))
            .expect(maxAge(-1))
            .expect(genID("123"))
            .expect(sessionBuilder("123", true, -1))
            .expect(session("123"))
            .expect(unsignedCookie("123"))
            .expect(sendCookie())
            .run(unit -> {
              new ServerSessionManager(unit.get(Config.class), unit.get(Session.Definition.class),
                  unit.get(Session.Store.class), unit.get(ParserExecutor.class))
                      .create(unit.get(Request.class), unit.get(Response.class));
            });
  }

  @Test
  public void createSignedCookieSession() throws Exception {
    new MockUnit(Config.class, Session.Definition.class, Cookie.Definition.class,
        Session.Store.class, ParserExecutor.class, Request.class, Response.class, SessionImpl.class)
            .expect(secret("querty"))
            .expect(cookie)
            .expect(saveInterval(-1L))
            .expect(maxAge(-1))
            .expect(genID("123"))
            .expect(sessionBuilder("123", true, -1))
            .expect(session("123"))
            .expect(signCookie("querty", "123", "signed"))
            .expect(sendCookie())
            .run(unit -> {
              new ServerSessionManager(unit.get(Config.class), unit.get(Session.Definition.class),
                  unit.get(Session.Store.class), unit.get(ParserExecutor.class))
                      .create(unit.get(Request.class), unit.get(Response.class));
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

  private Block secret(final String secret) {
    return unit -> {
      Config config = unit.get(Config.class);
      expect(config.hasPath("application.secret")).andReturn(true);
      expect(config.getString("application.secret")).andReturn(secret);
    };
  }

  private Block unsignedCookie(final String id) {
    return unit -> {
      Cookie.Definition cookie = unit.get(Cookie.Definition.class);
      Cookie.Definition newCookie = unit.constructor(Cookie.Definition.class)
          .build(cookie);

      expect(newCookie.value(id)).andReturn(newCookie);
      unit.registerMock(Cookie.Definition.class, newCookie);
    };
  }

  private Block sendCookie() {
    return unit -> {
      Cookie.Definition cookie = unit.get(Cookie.Definition.class);
      Response rsp = unit.get(Response.class);
      expect(rsp.cookie(cookie)).andReturn(rsp);
    };
  }

  private Block session(final String sid) {
    return unit -> {
      SessionImpl session = unit.get(SessionImpl.class);
      expect(session.id()).andReturn(sid);
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

  private Block genID(final String id) {
    return unit -> {
      Store store = unit.get(Session.Store.class);
      expect(store.generateID()).andReturn(id);
    };
  }

  private Block saveInterval(final Long saveInterval) {
    return unit -> {
      Definition session = unit.get(Session.Definition.class);
      expect(session.saveInterval()).andReturn(Optional.of(saveInterval));
    };
  }

  private Block maxAge(final Integer maxAge) {
    return unit -> {
      Cookie.Definition session = unit.get(Cookie.Definition.class);
      expect(session.maxAge()).andReturn(Optional.of(maxAge));
    };
  }
}
