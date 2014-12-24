package org.jooby.internal.undertow;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.SessionConfig;

import org.jooby.MockUnit;
import org.jooby.Session;
import org.jooby.Session.Builder;
import org.jooby.Session.Store;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({UndertowSessionManager.class, HttpServerExchange.class })
public class UndertowSessionManagerTest {

  @Test
  public void deploymentName() throws Exception {
    new MockUnit(Session.Store.class)
        .run(unit -> {
          Store store = unit.get(Session.Store.class);
          UndertowSessionManager sm = new UndertowSessionManager(store, 0, 0, null);
          assertEquals(store.getClass().getSimpleName(), sm.getDeploymentName());
        });
  }

  @Test
  public void emptyStart() throws Exception {
    new MockUnit(Session.Store.class)
        .run(unit -> {
          UndertowSessionManager sm = new UndertowSessionManager(unit.get(Session.Store.class), 0,
              0, null);
          sm.start();
        });
  }

  @Test
  public void emptyStop() throws Exception {
    new MockUnit(Session.Store.class)
        .run(unit -> {
          UndertowSessionManager sm = new UndertowSessionManager(unit.get(Session.Store.class), 0,
              0, null);
          sm.stop();
        });
  }

  @Test
  public void createSession() throws Exception {
    Session.Store store = new Store() {

      @Override
      public Session get(final Builder builder) {
        return null;
      }

      @Override
      public void save(final Session session, final SaveReason reason) {
      }

      @Override
      public void delete(final String id) {
      }

      @Override
      public String generateID(final long seed) {
        return "1234";
      }

    };
    new MockUnit(SessionConfig.class)
        .expect(unit -> {
          unit.registerMock(HttpServerExchange.class);
        })
        .expect(unit -> {
          SessionConfig config = unit.get(SessionConfig.class);
          expect(config.findSessionId(unit.get(HttpServerExchange.class))).andReturn(null);
          config.setSessionId(unit.get(HttpServerExchange.class), "1234");
        })
        .run(unit -> {
          UndertowSessionManager sm = new UndertowSessionManager(store, 0, 0, null);
          UndertowSession session = sm.createSession(unit.get(HttpServerExchange.class), unit.get(SessionConfig.class));
          assertNotNull(session);
        });
  }

  @Test(expected = RuntimeException.class)
  public void createSessionWithRuntimeErr() throws Exception {
    Session.Store store = new Store() {

      @Override
      public Session get(final Builder builder) {
        return null;
      }

      @Override
      public void save(final Session session, final SaveReason reason) {
      }

      @Override
      public void delete(final String id) {
      }

      @Override
      public String generateID(final long seed) {
        return "1234";
      }

    };

    SessionConfig config = new SessionConfig() {

      @Override
      public void setSessionId(final HttpServerExchange exchange, final String sessionId) {
        throw new RuntimeException("intentional err");
      }

      @Override
      public void clearSession(final HttpServerExchange exchange, final String sessionId) {
      }

      @Override
      public String findSessionId(final HttpServerExchange exchange) {
        return null;
      }

      @Override
      public SessionCookieSource sessionCookieSource(final HttpServerExchange exchange) {
        return null;
      }

      @Override
      public String rewriteUrl(final String originalUrl, final String sessionId) {
        return null;
      }}
    ;
    new MockUnit()
        .expect(unit -> {
          unit.registerMock(HttpServerExchange.class);
        })
        .run(unit -> {
          UndertowSessionManager sm = new UndertowSessionManager(store, 0, 0, null);
          sm.createSession(unit.get(HttpServerExchange.class), config);
        });
  }

}
