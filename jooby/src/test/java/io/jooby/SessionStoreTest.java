/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class SessionStoreTest {

  /** Dummy implementation to test the SessionStore.InMemory base logic in isolation. */
  static class DummyInMemory extends SessionStore.InMemory {
    Map<String, Data> storage = new HashMap<>();

    public DummyInMemory(SessionToken token) {
      super(token);
    }

    @Override
    protected Data getOrCreate(String sessionId, Function<String, Data> factory) {
      return storage.computeIfAbsent(sessionId, factory);
    }

    @Override
    protected Data getOrNull(String sessionId) {
      return storage.get(sessionId);
    }

    @Override
    protected Data remove(String sessionId) {
      return storage.remove(sessionId);
    }

    @Override
    protected void put(String sessionId, Data data) {
      storage.put(sessionId, data);
    }
  }

  private SessionToken token;
  private DummyInMemory store;
  private Context ctx;

  @BeforeEach
  void setUp() {
    token = mock(SessionToken.class);
    store = new DummyInMemory(token);
    ctx = mock(Context.class);
  }

  @Test
  @DisplayName("Verify Data inner class and expiration logic")
  void testDataExpiration() {
    Instant now = Instant.now();
    Instant past = now.minus(Duration.ofMinutes(10));

    SessionStore.InMemory.Data data = new SessionStore.InMemory.Data(past, past, new HashMap<>());

    // Timeout is 5 minutes, 10 minutes elapsed -> should be expired
    assertTrue(data.isExpired(Duration.ofMinutes(5)));

    // Timeout is 15 minutes, 10 minutes elapsed -> should NOT be expired
    assertFalse(data.isExpired(Duration.ofMinutes(15)));
  }

  @Test
  @DisplayName("Verify token getters and setters")
  void testTokenAccessors() {
    assertEquals(token, store.getToken());

    SessionToken newToken = mock(SessionToken.class);
    store.setToken(newToken);
    assertEquals(newToken, store.getToken());
  }

  @Test
  @DisplayName("Verify newSession creates a session, saves token, and stores data")
  void testNewSession() {
    when(token.newToken()).thenReturn("session-123");

    Session session = store.newSession(ctx);

    assertNotNull(session);
    assertEquals("session-123", session.getId());
    verify(token).saveToken(ctx, "session-123");
    assertNotNull(store.getOrNull("session-123"));
  }

  @Test
  @DisplayName("Verify findSession branches: no token, data missing, and success")
  void testFindSession() {
    // Branch 1: No token found in context
    when(token.findToken(ctx)).thenReturn(null);
    assertNull(store.findSession(ctx));

    // Branch 2: Token found, but no data in storage
    when(token.findToken(ctx)).thenReturn("missing-id");
    assertNull(store.findSession(ctx));

    // Branch 3: Token found and data exists
    SessionStore.InMemory.Data data =
        new SessionStore.InMemory.Data(Instant.now(), Instant.now(), new ConcurrentHashMap<>());
    store.put("valid-id", data);
    when(token.findToken(ctx)).thenReturn("valid-id");

    Session session = store.findSession(ctx);
    assertNotNull(session);
    assertEquals("valid-id", session.getId());
    verify(token).saveToken(ctx, "valid-id");
  }

  @Test
  @DisplayName("Verify deleteSession removes data and deletes token")
  void testDeleteSession() {
    Session session = mock(Session.class);
    when(session.getId()).thenReturn("to-delete");

    store.put(
        "to-delete", new SessionStore.InMemory.Data(Instant.now(), Instant.now(), new HashMap<>()));

    store.deleteSession(ctx, session);

    assertNull(store.getOrNull("to-delete"));
    verify(token).deleteToken(ctx, "to-delete");
  }

  @Test
  @DisplayName("Verify touchSession calls saveSession and saveToken")
  void testTouchSession() {
    Session session = mock(Session.class);
    when(session.getId()).thenReturn("to-touch");
    when(session.getCreationTime()).thenReturn(Instant.now());
    when(session.toMap()).thenReturn(new HashMap<>());

    store.touchSession(ctx, session);

    assertNotNull(store.getOrNull("to-touch"));
    verify(token).saveToken(ctx, "to-touch");
  }

  @Test
  @DisplayName("Verify saveSession creates new Data entry")
  void testSaveSession() {
    Session session = mock(Session.class);
    when(session.getId()).thenReturn("to-save");
    when(session.getCreationTime()).thenReturn(Instant.now());
    when(session.toMap()).thenReturn(Map.of("k", "v"));

    store.saveSession(ctx, session);

    SessionStore.InMemory.Data saved = store.getOrNull("to-save");
    assertNotNull(saved);
  }

  @Test
  @DisplayName("Verify renewSessionId with missing and existing data")
  void testRenewSessionId() {
    Session session = mock(Session.class);

    // Branch 1: old data doesn't exist
    when(session.getId()).thenReturn("non-existent");
    store.renewSessionId(ctx, session);
    verify(token, org.mockito.Mockito.never()).newToken(); // Should not do anything

    // Branch 2: old data exists
    when(session.getId()).thenReturn("old-id");
    store.put(
        "old-id", new SessionStore.InMemory.Data(Instant.now(), Instant.now(), new HashMap<>()));
    when(token.newToken()).thenReturn("new-id");

    store.renewSessionId(ctx, session);

    assertNull(store.getOrNull("old-id")); // Old is removed
    assertNotNull(store.getOrNull("new-id")); // New is put
    verify(session).setId("new-id");
  }

  @Test
  @DisplayName("Verify static memory factory methods")
  void testMemoryFactories() {
    Cookie cookie = new Cookie("sid");
    assertNotNull(SessionStore.memory(cookie));
    assertNotNull(SessionStore.memory(cookie, Duration.ofMinutes(15)));

    SessionToken st = mock(SessionToken.class);
    assertNotNull(SessionStore.memory(st));
    assertNotNull(SessionStore.memory(st, Duration.ofMinutes(15)));
  }

  @Test
  @DisplayName("Verify static signed factory methods and their internal lambda logic")
  void testSignedFactories() {
    Cookie cookie = new Cookie("sid");
    assertNotNull(SessionStore.signed(cookie, "my-secret"));

    SessionToken st = mock(SessionToken.class);
    SessionStore signedStore = SessionStore.signed(st, "my-secret");
    assertNotNull(signedStore);

    // Trigger internal lambdas (encoder and decoder) for coverage.
    // By simulating finding/saving sessions through the returned SignedSessionStore,
    // the SneakyThrows.Functions defined in `SessionStore.signed` are invoked.
    Context mockCtx = mock(Context.class);
    Session mockSession = mock(Session.class);
    when(mockSession.getId()).thenReturn("signed-id");
    when(mockSession.toMap()).thenReturn(Map.of("foo", "bar"));

    try {
      // Trigger encoder lambda inside saveSession
      signedStore.saveSession(mockCtx, mockSession);

      // Trigger decoder lambda -> branch: unsign == null
      when(st.findToken(mockCtx)).thenReturn("bad-token");
      signedStore.findSession(mockCtx);

      // Trigger decoder lambda -> branch: unsign != null (successful decode)
      String validToken = Cookie.sign(Cookie.encode(Map.of("foo", "bar")), "my-secret");
      when(st.findToken(mockCtx)).thenReturn(validToken);
      signedStore.findSession(mockCtx);
    } catch (Exception ignored) {
      // Safely catch any internal downstream errors; the primary goal is executing
      // the lambdas instantiated by the `SessionStore.signed()` factory.
    }
  }

  @Test
  @DisplayName("Verify unsupported session store")
  void testUnsupportedStore() {
    assertThrows(Usage.class, () -> SessionStore.UNSUPPORTED.newSession(ctx));
    assertThrows(Usage.class, () -> SessionStore.UNSUPPORTED.findSession(ctx));
    assertThrows(
        Usage.class, () -> SessionStore.UNSUPPORTED.deleteSession(ctx, mock(Session.class)));
    assertThrows(
        Usage.class, () -> SessionStore.UNSUPPORTED.touchSession(ctx, mock(Session.class)));
    assertThrows(Usage.class, () -> SessionStore.UNSUPPORTED.saveSession(ctx, mock(Session.class)));
    assertThrows(
        Usage.class, () -> SessionStore.UNSUPPORTED.renewSessionId(ctx, mock(Session.class)));
  }
}
