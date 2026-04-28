/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jooby.Context;
import io.jooby.Router;
import io.jooby.Session;
import io.jooby.SessionStore;
import io.jooby.value.Value;
import io.jooby.value.ValueFactory;

public class SessionImplTest {

  private Context ctx;
  private SessionStore store;
  private SessionImpl session;
  private final String sessionId = "session-123";

  @BeforeEach
  void setUp() {
    ctx = mock(Context.class);
    Router router = mock(Router.class);
    store = mock(SessionStore.class);

    when(ctx.getRouter()).thenReturn(router);
    when(router.getSessionStore()).thenReturn(store);
    when(ctx.getValueFactory()).thenReturn(new ValueFactory());

    session = new SessionImpl(ctx, sessionId);
  }

  @Test
  @DisplayName("Verify session ID and status flags (New/Modify)")
  void testStatusFlags() {
    assertEquals(sessionId, session.getId());

    session.setNew(true);
    assertTrue(session.isNew());

    assertFalse(session.isModify());
    session.setModify(true);
    assertTrue(session.isModify());

    session.setId("new-id");
    assertEquals("new-id", session.getId());
  }

  @Test
  @DisplayName("Verify attribute manipulation and state updates")
  void testAttributes() {
    session.put("key", "value");

    // Verify updateState side effects
    assertTrue(session.isModify());
    verify(store).touchSession(ctx, session);

    Value val = session.get("key");
    assertEquals("value", val.value());

    // Test put(String, Object) which calls toString()
    session.put("int", 100);
    assertEquals("100", session.get("int").value());

    // Test remove
    Value removed = session.remove("key");
    assertEquals("value", removed.value());
    assertTrue(session.get("key").isMissing());

    // Test clear
    session.clear();
    assertTrue(session.toMap().isEmpty());
  }

  @Test
  @DisplayName("Verify time tracking accessors")
  void testTimeTracking() {
    Instant now = Instant.now();

    session.setCreationTime(now);
    assertEquals(now, session.getCreationTime());

    session.setLastAccessedTime(now);
    assertEquals(now, session.getLastAccessedTime());
  }

  @Test
  @DisplayName("Verify session lifecycle: Renew and Destroy")
  void testLifecycle() {
    Map<String, Object> attributes = new HashMap<>();
    when(ctx.getAttributes()).thenReturn(attributes);

    // Renew ID
    session.renewId();
    verify(store).renewSessionId(ctx, session);
    assertTrue(session.isModify());

    // Destroy
    session.destroy();
    verify(store).deleteSession(ctx, session);
    assertFalse(attributes.containsKey(Session.NAME));
  }

  @Test
  @DisplayName("Verify secondary constructor with initial attributes")
  void testAttributeConstructor() {
    Map<String, String> initial = new HashMap<>();
    initial.put("foo", "bar");

    SessionImpl sessionWithAttrs = new SessionImpl(ctx, "abc", initial);
    assertEquals("bar", sessionWithAttrs.get("foo").value());
  }
}
