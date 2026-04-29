/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.pac4j;

import static io.jooby.internal.pac4j.Pac4jSession.BIN;
import static io.jooby.internal.pac4j.Pac4jSession.PAC4J;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.exception.http.BadRequestAction;
import org.pac4j.core.exception.http.ForbiddenAction;
import org.pac4j.core.exception.http.FoundAction;
import org.pac4j.core.exception.http.HttpAction;
import org.pac4j.core.exception.http.NoContentAction;
import org.pac4j.core.exception.http.OkAction;
import org.pac4j.core.exception.http.SeeOtherAction;
import org.pac4j.core.exception.http.StatusAction;
import org.pac4j.core.exception.http.UnauthorizedAction;
import org.pac4j.core.util.serializer.Serializer;

import io.jooby.Context;
import io.jooby.Session;
import io.jooby.pac4j.Pac4jContext;
import io.jooby.value.Value;

@ExtendWith(MockitoExtension.class)
class SessionStoreImplTest {

  interface TestWebContext extends WebContext, Pac4jContext {}

  @Mock TestWebContext webContext;
  @Mock Context ctx;
  @Mock Session session;
  @Mock Serializer serializer;

  private SessionStoreImpl store;

  @BeforeEach
  void setUp() {
    store = new SessionStoreImpl();
    lenient().when(webContext.getContext()).thenReturn(ctx);
  }

  @Test
  void testGetSessionIdCreateTrue() {
    when(ctx.session()).thenReturn(session);
    when(session.getId()).thenReturn("sess-1");

    Optional<String> id = store.getSessionId(webContext, true);
    assertEquals(Optional.of("sess-1"), id);
  }

  @Test
  void testGetSessionIdCreateFalseSessionExists() {
    when(ctx.sessionOrNull()).thenReturn(session);
    when(session.getId()).thenReturn("sess-2");

    Optional<String> id = store.getSessionId(webContext, false);
    assertEquals(Optional.of("sess-2"), id);
  }

  @Test
  void testGetSessionIdCreateFalseNoSession() {
    when(ctx.sessionOrNull()).thenReturn(null);

    Optional<String> id = store.getSessionId(webContext, false);
    assertEquals(Optional.empty(), id);
  }

  @Test
  void testGetNoSession() {
    when(ctx.sessionOrNull()).thenReturn(null);

    Optional<Object> val = store.get(webContext, "key");
    assertEquals(Optional.empty(), val);
  }

  @Test
  void testGetSessionHasMissingNode() {
    when(ctx.sessionOrNull()).thenReturn(session);
    Value node = mock(Value.class);
    when(node.isMissing()).thenReturn(true);
    when(session.get("key")).thenReturn(node);

    Optional<Object> val = store.get(webContext, "key");
    assertEquals(Optional.empty(), val);
  }

  @Test
  void testGetSessionHasPlainString() {
    when(ctx.sessionOrNull()).thenReturn(session);
    Value node = mock(Value.class);
    when(node.isMissing()).thenReturn(false);
    when(node.value()).thenReturn("plain-value");
    when(session.get("key")).thenReturn(node);

    Optional<Object> val = store.get(webContext, "key");
    assertEquals(Optional.of("plain-value"), val);
  }

  @Test
  void testGetSessionHasBinSerializedObject() {
    when(ctx.sessionOrNull()).thenReturn(session);
    Value node = mock(Value.class);
    when(node.isMissing()).thenReturn(false);
    when(node.value()).thenReturn(BIN + "encoded");
    when(session.get("key")).thenReturn(node);

    when(ctx.require(Serializer.class)).thenReturn(serializer);
    Object deserialized = new Object();
    when(serializer.deserializeFromString("encoded")).thenReturn(deserialized);

    Optional<Object> val = store.get(webContext, "key");
    assertEquals(Optional.of(deserialized), val);
  }

  @Test
  void testGetSessionHasPac4jHttpAction() {
    when(ctx.sessionOrNull()).thenReturn(session);
    Value node = mock(Value.class);
    when(node.isMissing()).thenReturn(false);
    when(node.value()).thenReturn(PAC4J + "400"); // BadRequest
    when(session.get("key")).thenReturn(node);

    Optional<Object> val = store.get(webContext, "key");
    assertTrue(val.isPresent());
    assertTrue(val.get() instanceof BadRequestAction);
  }

  @Test
  void testSetNullOrEmptyRemovesFromSessionIfPresent() {
    when(ctx.sessionOrNull()).thenReturn(session);

    store.set(webContext, "key1", null);
    verify(session).remove("key1");

    store.set(webContext, "key2", "");
    verify(session).remove("key2");
  }

  @Test
  void testSetNullOrEmptyDoesNothingIfNoSession() {
    when(ctx.sessionOrNull()).thenReturn(null);

    store.set(webContext, "key", null);
    verify(ctx, never()).session(); // Never forces creation
  }

  @Test
  void testSetPrimitiveObject() {
    when(ctx.session()).thenReturn(session);

    store.set(webContext, "key1", "string-val");
    verify(session).put("key1", "string-val");

    store.set(webContext, "key2", 42);
    verify(session).put("key2", "42");

    store.set(webContext, "key3", true);
    verify(session).put("key3", "true");
  }

  @Test
  void testSetComplexObjectUsesSerializer() {
    when(ctx.session()).thenReturn(session);
    when(ctx.require(Serializer.class)).thenReturn(serializer);

    Object complex = new Object();
    when(serializer.serializeToString(complex)).thenReturn("serialized-data");

    store.set(webContext, "key", complex);
    verify(session).put("key", BIN + "serialized-data");
  }

  @Test
  void testSetHttpAction() {
    when(ctx.session()).thenReturn(session);

    store.set(webContext, "key1", new OkAction("ok content"));
    verify(session).put("key1", PAC4J + "200:ok content");

    store.set(webContext, "key2", new FoundAction("/redirect"));
    verify(session).put("key2", PAC4J + "302:/redirect");
  }

  @Test
  void testDestroySession() {
    when(ctx.sessionOrNull()).thenReturn(session);
    assertTrue(store.destroySession(webContext));
    verify(session).destroy();

    when(ctx.sessionOrNull()).thenReturn(null);
    assertFalse(store.destroySession(webContext));
  }

  @Test
  void testGetTrackableSession() {
    when(ctx.sessionOrNull()).thenReturn(session);
    assertEquals(Optional.of(session), store.getTrackableSession(webContext));
  }

  @Test
  void testBuildFromTrackableSession() {
    assertTrue(store.buildFromTrackableSession(webContext, new Object()).isPresent());
    assertFalse(store.buildFromTrackableSession(webContext, null).isPresent());
  }

  @Test
  void testRenewSession() {
    when(ctx.sessionOrNull()).thenReturn(session);
    assertTrue(store.renewSession(webContext));
    verify(session).renewId();

    when(ctx.sessionOrNull()).thenReturn(null);
    assertFalse(store.renewSession(webContext));
  }

  // --- HttpAction string mapping coverage tests ---

  @Test
  void testHttpActionEncodingDecoding() {
    assertDecode(PAC4J + "400", BadRequestAction.class);
    assertDecode(PAC4J + "403", ForbiddenAction.class);
    assertDecode(PAC4J + "302:/location", FoundAction.class);
    assertDecode(PAC4J + "307:/temporary", FoundAction.class);
    assertDecode(PAC4J + "204", NoContentAction.class);
    assertDecode(PAC4J + "200:body", OkAction.class);
    assertDecode(PAC4J + "303:/seeother", SeeOtherAction.class);
    assertDecode(PAC4J + "401", UnauthorizedAction.class);
    assertDecode(PAC4J + "500", StatusAction.class); // default case
  }

  private void assertDecode(String encoded, Class<? extends HttpAction> expectedActionClass) {
    Value node = mock(Value.class);
    when(node.isMissing()).thenReturn(false);
    when(node.value()).thenReturn(encoded);

    Optional<Object> decoded = SessionStoreImpl.strToObject(ctx, node);
    assertTrue(decoded.isPresent());
    assertNotNull(expectedActionClass.cast(decoded.get()));
  }
}
