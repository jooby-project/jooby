/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.pac4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.jooby.Context;
import io.jooby.Session;
import io.jooby.pac4j.Pac4jUntrustedDataFound;
import io.jooby.value.Value;

@ExtendWith(MockitoExtension.class)
class Pac4jSessionTest {

  @Mock private Session mockSession;
  @Mock private Context mockContext;

  private Pac4jSession pac4jSession;

  @BeforeEach
  void setUp() {
    pac4jSession = new Pac4jSession(mockSession);
  }

  @Test
  void testGetId() {
    when(mockSession.getId()).thenReturn("sess-123");
    assertEquals("sess-123", pac4jSession.getId());
  }

  @Test
  void testGet() {
    Value mockValue = mock(Value.class);
    when(mockSession.get("key")).thenReturn(mockValue);
    assertSame(mockValue, pac4jSession.get("key"));
  }

  @Test
  void testGetLastAccessedTime() {
    Instant now = Instant.now();
    when(mockSession.getLastAccessedTime()).thenReturn(now);
    assertSame(now, pac4jSession.getLastAccessedTime());
  }

  @Test
  void testDestroy() {
    pac4jSession.destroy();
    verify(mockSession).destroy();
  }

  @Test
  void testSetId() {
    assertSame(pac4jSession, pac4jSession.setId("new-id"));
    verify(mockSession).setId("new-id");
  }

  @Test
  void testRemove() {
    Value mockValue = mock(Value.class);
    when(mockSession.remove("key")).thenReturn(mockValue);
    assertSame(mockValue, pac4jSession.remove("key"));
  }

  @Test
  void testIsNew() {
    when(mockSession.isNew()).thenReturn(true);
    assertTrue(pac4jSession.isNew());
  }

  @Test
  void testSetNew() {
    assertSame(pac4jSession, pac4jSession.setNew(false));
    verify(mockSession).setNew(false);
  }

  @Test
  void testSetLastAccessedTime() {
    Instant time = Instant.now();
    assertSame(pac4jSession, pac4jSession.setLastAccessedTime(time));
    verify(mockSession).setLastAccessedTime(time);
  }

  @Test
  void testIsModify() {
    when(mockSession.isModify()).thenReturn(true);
    assertTrue(pac4jSession.isModify());
  }

  @Test
  void testSetCreationTime() {
    Instant time = Instant.now();
    assertSame(pac4jSession, pac4jSession.setCreationTime(time));
    verify(mockSession).setCreationTime(time);
  }

  @Test
  void testSetModify() {
    assertSame(pac4jSession, pac4jSession.setModify(true));
    verify(mockSession).setModify(true);
  }

  @Test
  void testRenewId() {
    assertSame(pac4jSession, pac4jSession.renewId());
    verify(mockSession).renewId();
  }

  @Test
  void testGetCreationTime() {
    Instant time = Instant.now();
    when(mockSession.getCreationTime()).thenReturn(time);
    assertSame(time, pac4jSession.getCreationTime());
  }

  @Test
  void testToMap() {
    Map<String, String> map = Map.of("k", "v");
    when(mockSession.toMap()).thenReturn(map);
    assertSame(map, pac4jSession.toMap());
  }

  @Test
  void testClear() {
    assertSame(pac4jSession, pac4jSession.clear());
    verify(mockSession).clear();
  }

  @Test
  void testGetSession() {
    assertSame(mockSession, pac4jSession.getSession());
  }

  // --- Untrusted Data Prevention (put) ---

  @Test
  void testPutNullValueAllowed() {
    assertSame(pac4jSession, pac4jSession.put("key", (String) null));
    verify(mockSession).put("key", (String) null);
  }

  @Test
  void testPutSafeValueAllowed() {
    assertSame(pac4jSession, pac4jSession.put("key", "safe-value"));
    verify(mockSession).put("key", "safe-value");
  }

  @Test
  void testPutPac4jPrefixThrowsException() {
    assertThrows(
        Pac4jUntrustedDataFound.class,
        () -> {
          pac4jSession.put("key", Pac4jSession.PAC4J + "malicious");
        });
  }

  @Test
  void testPutBinPrefixThrowsException() {
    assertThrows(
        Pac4jUntrustedDataFound.class,
        () -> {
          pac4jSession.put("key", Pac4jSession.BIN + "malicious");
        });
  }

  // --- ForwardingContext creation ---

  @Test
  void testCreateForwardingContextSession() {
    when(mockContext.session()).thenReturn(mockSession);

    Context forwardingContext = Pac4jSession.create(mockContext);
    Session sessionFromContext = forwardingContext.session();

    assertTrue(sessionFromContext instanceof Pac4jSession);
    assertSame(mockSession, ((Pac4jSession) sessionFromContext).getSession());
  }

  @Test
  void testCreateForwardingContextSessionOrNull_WhenPresent() {
    when(mockContext.sessionOrNull()).thenReturn(mockSession);

    Context forwardingContext = Pac4jSession.create(mockContext);
    Session sessionFromContext = forwardingContext.sessionOrNull();

    assertTrue(sessionFromContext instanceof Pac4jSession);
    assertSame(mockSession, ((Pac4jSession) sessionFromContext).getSession());
  }

  @Test
  void testCreateForwardingContextSessionOrNull_WhenNull() {
    when(mockContext.sessionOrNull()).thenReturn(null);

    Context forwardingContext = Pac4jSession.create(mockContext);
    Session sessionFromContext = forwardingContext.sessionOrNull();

    assertNull(sessionFromContext);
  }
}
