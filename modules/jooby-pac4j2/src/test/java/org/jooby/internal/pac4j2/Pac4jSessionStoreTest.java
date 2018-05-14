package org.jooby.internal.pac4j2;

import com.google.common.collect.Lists;
import static org.easymock.EasyMock.expect;
import org.jooby.Mutant;
import org.jooby.Request;
import org.jooby.Session;
import org.jooby.test.MockUnit;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.pac4j.core.context.WebContext;

import java.util.Optional;

public class Pac4jSessionStoreTest {

  private MockUnit.Block session = unit -> {
    Session session = unit.get(Session.class);
    Request request = unit.get(Request.class);
    expect(request.session()).andReturn(session);
  };

  @Test
  public void newSessionStore() throws Exception {
    new MockUnit(Request.class)
        .run(unit -> {
          new Pac4jSessionStore(unit.get(Request.class));
        });
  }

  @Test
  public void getOrCreateSessionId() throws Exception {
    new MockUnit(Request.class, WebContext.class, Session.class)
        .expect(session)
        .expect(sessionId("sid"))
        .run(unit -> {
          Pac4jSessionStore session = new Pac4jSessionStore(unit.get(Request.class));
          session.getOrCreateSessionId(unit.get(WebContext.class));
        });
  }

  @Test
  public void getShouldReturnNullIfNoSessionExists() throws Exception {
    new MockUnit(Request.class, WebContext.class, Session.class)
        .expect(ifSession(false))
        .run(unit -> {
          Pac4jSessionStore session = new Pac4jSessionStore(unit.get(Request.class));
          assertEquals(null, session.get(unit.get(WebContext.class), "attribute"));
        });
  }

  @Test
  public void getShouldReadSimpleSessionAttribute() throws Exception {
    new MockUnit(Request.class, WebContext.class, Session.class)
        .expect(ifSession(true))
        .expect(ifSession(true))
        .expect(getSessionAttribute("attribute", "value"))
        .expect(getSessionAttribute("missing", null))
        .run(unit -> {
          Pac4jSessionStore session = new Pac4jSessionStore(unit.get(Request.class));
          assertEquals("value", session.get(unit.get(WebContext.class), "attribute"));
          assertEquals(null, session.get(unit.get(WebContext.class), "missing"));
        });
  }

  @Test
  public void getShouldReadComplexSessionAttribute() throws Exception {
    String serializedValue = "b64~rO0ABXNyABNqYXZhLnV0aWwuQXJyYXlMaXN0eIHSHZnHYZ0DAAFJAARzaXpleHAAAAADdwQAAAADc3IAEWphdmEubGFuZy5JbnRlZ2VyEuKgpPeBhzgCAAFJAAV2YWx1ZXhyABBqYXZhLmxhbmcuTnVtYmVyhqyVHQuU4IsCAAB4cAAAAAZzcQB+AAIAAAAHc3EAfgACAAAACHg=";
    new MockUnit(Request.class, WebContext.class, Session.class)
        .expect(ifSession(true))
        .expect(getSessionAttribute("values", serializedValue))
        .run(unit -> {
          Pac4jSessionStore session = new Pac4jSessionStore(unit.get(Request.class));
          assertEquals(Lists.newArrayList(6, 7, 8),
              session.get(unit.get(WebContext.class), "values"));
        });
  }

  @Test
  public void shouldRemoveNullValuesFromWhenSessionPresent() throws Exception {
    new MockUnit(Request.class, WebContext.class, Session.class)
        .expect(ifSession(false))
        .expect(ifSession(true))
        .expect(setSessionAttribute("missing", null))
        .run(unit -> {
          Pac4jSessionStore session = new Pac4jSessionStore(unit.get(Request.class));
          session.set(unit.get(WebContext.class), "missing", null);
          session.set(unit.get(WebContext.class), "missing", null);
        });
  }

  @Test
  public void shouldSetSessionAttribute() throws Exception {
    new MockUnit(Request.class, WebContext.class, Session.class)
        .expect(session)
        .expect(setSessionAttribute("attribute", "value"))
        .run(unit -> {
          Pac4jSessionStore session = new Pac4jSessionStore(unit.get(Request.class));
          session.set(unit.get(WebContext.class), "attribute", "value");
        });
  }

  @Test
  public void setShouldSerializeObjects() throws Exception {
    String serializedValue = "b64~rO0ABXNyABNqYXZhLnV0aWwuQXJyYXlMaXN0eIHSHZnHYZ0DAAFJAARzaXpleHAAAAADdwQAAAADc3IAEWphdmEubGFuZy5JbnRlZ2VyEuKgpPeBhzgCAAFJAAV2YWx1ZXhyABBqYXZhLmxhbmcuTnVtYmVyhqyVHQuU4IsCAAB4cAAAAAZzcQB+AAIAAAAHc3EAfgACAAAACHg=";
    new MockUnit(Request.class, WebContext.class, Session.class)
        .expect(session)
        .expect(setSessionAttribute("values", serializedValue))
        .run(unit -> {
          Pac4jSessionStore session = new Pac4jSessionStore(unit.get(Request.class));
          session.set(unit.get(WebContext.class), "values", Lists.newArrayList(6, 7, 8));
        });
  }

  @Test
  public void destroySessionIfPresent() throws Exception {
    new MockUnit(Request.class, WebContext.class, Session.class)
        .expect(ifSession(true))
        .expect(ifSession(false))
        .expect(unit -> {
          Session session = unit.get(Session.class);
          session.destroy();
        })
        .run(unit -> {
          Pac4jSessionStore session = new Pac4jSessionStore(unit.get(Request.class));
          assertEquals(true, session.destroySession(unit.get(WebContext.class)));
          assertEquals(true, session.destroySession(unit.get(WebContext.class)));
        });
  }

  @Test
  public void trackableSession() throws Exception {
    new MockUnit(Request.class, WebContext.class, Session.class)
        .run(unit -> {
          Pac4jSessionStore session = new Pac4jSessionStore(unit.get(Request.class));
          assertEquals(unit.get(Request.class),
              session.getTrackableSession(unit.get(WebContext.class)));
        });
  }

  @Test
  public void fromTrackableSession() throws Exception {
    new MockUnit(Request.class, WebContext.class, Session.class)
        .run(unit -> {
          Pac4jSessionStore session = new Pac4jSessionStore(unit.get(Request.class));
          assertTrue(session.buildFromTrackableSession(unit.get(WebContext.class),
              unit.get(Request.class)) instanceof Pac4jSessionStore);
          assertEquals(null,
              session.buildFromTrackableSession(unit.get(WebContext.class), new Object()));
        });
  }

  @Test
  public void renewSessionId() throws Exception {
    new MockUnit(Request.class, WebContext.class, Session.class)
        .expect(ifSession(true))
        .expect(unit -> {
          Session session = unit.get(Session.class);
          expect(session.renewId()).andReturn(session);
        })
        .run(unit -> {
          Pac4jSessionStore session = new Pac4jSessionStore(unit.get(Request.class));
          assertEquals(true, session.renewSession(unit.get(WebContext.class)));
        });
  }

  @Test
  public void shouldIgnoreRenewSessionIfNoSession() throws Exception {
    new MockUnit(Request.class, WebContext.class, Session.class)
        .expect(ifSession(false))
        .run(unit -> {
          Pac4jSessionStore session = new Pac4jSessionStore(unit.get(Request.class));
          assertEquals(true, session.renewSession(unit.get(WebContext.class)));
        });
  }

  @Test
  public void sessionAttributeWithPrimitiveWrappers() {
    assertEquals("1", Pac4jSessionStore.objToStr(1));
    assertEquals("1.5", Pac4jSessionStore.objToStr(new Double(1.5)));
  }

  private MockUnit.Block getSessionAttribute(String name, String value) {
    return unit -> {
      Mutant attribute = unit.mock(Mutant.class);
      expect(attribute.toOptional()).andReturn(Optional.ofNullable(value));

      Session session = unit.get(Session.class);
      expect(session.get(name)).andReturn(attribute);
    };
  }

  private MockUnit.Block setSessionAttribute(String name, String value) {
    return unit -> {
      Session session = unit.get(Session.class);
      if (value == null) {
        expect(session.unset(name)).andReturn(null);
      } else {
        expect(session.set(name, value)).andReturn(session);
      }
    };
  }

  private MockUnit.Block sessionId(String sid) {
    return unit -> {
      Session session = unit.get(Session.class);
      expect(session.id()).andReturn(sid);
    };
  }

  private MockUnit.Block ifSession(boolean ifSession) {
    return unit -> {
      Request request = unit.get(Request.class);
      Optional<Session> session = Optional.empty();
      if (ifSession) {
        session = Optional.of(unit.get(Session.class));
      }
      expect(request.ifSession()).andReturn(session);
    };
  }

}
