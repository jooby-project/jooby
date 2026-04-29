/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jooby.Context;
import io.jooby.Router;
import io.jooby.Session;
import io.jooby.exception.InvalidCsrfToken;
import io.jooby.value.Value;

public class CsrfHandlerTest {

  private Context ctx;
  private Session session;
  private Value sessionValue;

  @BeforeEach
  void setUp() {
    ctx = mock(Context.class);
    session = mock(Session.class);
    sessionValue = mock(Value.class);

    when(ctx.session()).thenReturn(session);
    when(session.get("csrf")).thenReturn(sessionValue);
  }

  @Test
  @DisplayName("Verify DEFAULT_FILTER branches across HTTP methods")
  void testDefaultFilter() {
    Context getCtx = mock(Context.class);
    when(getCtx.getMethod()).thenReturn(Router.GET);
    assertFalse(CsrfHandler.DEFAULT_FILTER.test(getCtx));

    Context postCtx = mock(Context.class);
    when(postCtx.getMethod()).thenReturn(Router.POST);
    assertTrue(CsrfHandler.DEFAULT_FILTER.test(postCtx));

    Context deleteCtx = mock(Context.class);
    when(deleteCtx.getMethod()).thenReturn(Router.DELETE);
    assertTrue(CsrfHandler.DEFAULT_FILTER.test(deleteCtx));

    Context patchCtx = mock(Context.class);
    when(patchCtx.getMethod()).thenReturn(Router.PATCH);
    assertTrue(CsrfHandler.DEFAULT_FILTER.test(patchCtx));

    Context putCtx = mock(Context.class);
    when(putCtx.getMethod()).thenReturn(Router.PUT);
    assertTrue(CsrfHandler.DEFAULT_FILTER.test(putCtx));
  }

  @Test
  @DisplayName("Verify DEFAULT_GENERATOR produces a UUID string")
  void testDefaultGenerator() {
    String token = CsrfHandler.DEFAULT_GENERATOR.apply(ctx);
    assertNotNull(token);
    assertEquals(36, token.length(), "UUIDs should be 36 characters long");
  }

  @Test
  @DisplayName("Verify new token generation when session token is absent")
  void testNewTokenGeneration() throws Exception {
    when(sessionValue.toOptional()).thenReturn(Optional.empty());

    CsrfHandler handler = new CsrfHandler();
    // Bypass verification to isolate generation logic
    handler.setRequestFilter(c -> false);

    handler.apply(ctx);

    verify(session).put(eq("csrf"), anyString());
    verify(ctx).setAttribute(eq("csrf"), anyString());
  }

  @Test
  @DisplayName("Verify existing token is used when present in session")
  void testExistingToken() throws Exception {
    when(sessionValue.toOptional()).thenReturn(Optional.of("existing-token"));

    CsrfHandler handler = new CsrfHandler();
    handler.setRequestFilter(c -> false);

    handler.apply(ctx);

    verify(session, never()).put(anyString(), anyString());
    verify(ctx).setAttribute("csrf", "existing-token");
  }

  @Test
  @DisplayName(
      "Verify token verification cascades successfully from Header, Cookie, Form, and Query")
  void testTokenVerificationSuccess() throws Exception {
    when(sessionValue.toOptional()).thenReturn(Optional.of("valid-token"));

    Value missing = mock(Value.class);
    when(missing.valueOrNull()).thenReturn(null);

    Value present = mock(Value.class);
    when(present.valueOrNull()).thenReturn("valid-token");

    CsrfHandler handler = new CsrfHandler();
    handler.setRequestFilter(c -> true); // Force verification for all runs

    // 1. Success via Header
    when(ctx.header("csrf")).thenReturn(present);
    when(ctx.cookie("csrf")).thenReturn(missing);
    when(ctx.form("csrf")).thenReturn(missing);
    when(ctx.query("csrf")).thenReturn(missing);
    handler.apply(ctx);

    // 2. Success via Cookie (Header missing)
    when(ctx.header("csrf")).thenReturn(missing);
    when(ctx.cookie("csrf")).thenReturn(present);
    handler.apply(ctx);

    // 3. Success via Form (Header, Cookie missing)
    when(ctx.cookie("csrf")).thenReturn(missing);
    when(ctx.form("csrf")).thenReturn(present);
    handler.apply(ctx);

    // 4. Success via Query (Header, Cookie, Form missing)
    when(ctx.form("csrf")).thenReturn(missing);
    when(ctx.query("csrf")).thenReturn(present);
    handler.apply(ctx);
  }

  @Test
  @DisplayName("Verify InvalidCsrfToken is thrown on mismatch")
  void testTokenVerificationMismatch() {
    when(sessionValue.toOptional()).thenReturn(Optional.of("valid-token"));

    Value missing = mock(Value.class);
    when(missing.valueOrNull()).thenReturn(null);

    Value invalid = mock(Value.class);
    when(invalid.valueOrNull()).thenReturn("invalid-token");

    when(ctx.header("csrf")).thenReturn(invalid);
    when(ctx.cookie("csrf")).thenReturn(missing);
    when(ctx.form("csrf")).thenReturn(missing);
    when(ctx.query("csrf")).thenReturn(missing);

    CsrfHandler handler = new CsrfHandler();
    handler.setRequestFilter(c -> true); // Force verification

    assertThrows(InvalidCsrfToken.class, () -> handler.apply(ctx));
  }

  @Test
  @DisplayName("Verify InvalidCsrfToken is thrown when no token is provided by client")
  void testTokenVerificationMissingClientToken() {
    when(sessionValue.toOptional()).thenReturn(Optional.of("valid-token"));

    Value missing = mock(Value.class);
    when(missing.valueOrNull()).thenReturn(null);

    // Provide no token in any request location
    when(ctx.header("csrf")).thenReturn(missing);
    when(ctx.cookie("csrf")).thenReturn(missing);
    when(ctx.form("csrf")).thenReturn(missing);
    when(ctx.query("csrf")).thenReturn(missing);

    CsrfHandler handler = new CsrfHandler();
    handler.setRequestFilter(c -> true); // Force verification

    assertThrows(InvalidCsrfToken.class, () -> handler.apply(ctx));
  }

  @Test
  @DisplayName("Verify custom token generator, custom name, and fluid setters")
  void testCustomGeneratorAndSetters() throws Exception {
    when(sessionValue.toOptional()).thenReturn(Optional.empty());

    CsrfHandler handler = new CsrfHandler("custom-csrf");

    // Verify fluent API returns 'this'
    CsrfHandler returned1 = handler.setTokenGenerator(c -> "my-custom-token");
    CsrfHandler returned2 = handler.setRequestFilter(c -> false);

    assertSame(handler, returned1);
    assertSame(handler, returned2);

    // Adjust mock behavior for the custom name
    Value customSessionValue = mock(Value.class);
    when(customSessionValue.toOptional()).thenReturn(Optional.empty());
    when(session.get("custom-csrf")).thenReturn(customSessionValue);

    handler.apply(ctx);

    verify(session).put("custom-csrf", "my-custom-token");
    verify(ctx).setAttribute("custom-csrf", "my-custom-token");
  }
}
