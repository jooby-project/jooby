/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jooby.Context;
import io.jooby.SessionToken;

public class MultipleSessionTokenTest {

  @Test
  @DisplayName("Verify findToken returns the first non-null token from available strategies")
  void testFindToken() {
    Context ctx = mock(Context.class);
    SessionToken t1 = mock(SessionToken.class);
    SessionToken t2 = mock(SessionToken.class);
    SessionToken t3 = mock(SessionToken.class);

    MultipleSessionToken multi = new MultipleSessionToken(t1, t2, t3);

    // Case 1: First strategy matches - should short-circuit and not call others
    when(t1.findToken(ctx)).thenReturn("token1");
    assertEquals("token1", multi.findToken(ctx));
    verify(t2, never()).findToken(ctx);

    // Case 2: Second strategy matches
    when(t1.findToken(ctx)).thenReturn(null);
    when(t2.findToken(ctx)).thenReturn("token2");
    assertEquals("token2", multi.findToken(ctx));

    // Case 3: No strategy matches
    when(t2.findToken(ctx)).thenReturn(null);
    when(t3.findToken(ctx)).thenReturn(null);
    assertNull(multi.findToken(ctx));
  }

  @Test
  @DisplayName("Verify saveToken propagates to all strategies if no existing token is found")
  void testSaveToken_NoneFound() {
    Context ctx = mock(Context.class);
    SessionToken t1 = mock(SessionToken.class);
    SessionToken t2 = mock(SessionToken.class);
    MultipleSessionToken multi = new MultipleSessionToken(t1, t2);

    // If findToken returns null for all, strategy() returns the full list
    when(t1.findToken(ctx)).thenReturn(null);
    when(t2.findToken(ctx)).thenReturn(null);

    multi.saveToken(ctx, "sid");

    verify(t1).saveToken(ctx, "sid");
    verify(t2).saveToken(ctx, "sid");
  }

  @Test
  @DisplayName("Verify saveToken only updates strategies where a token already exists")
  void testSaveToken_SomeFound() {
    Context ctx = mock(Context.class);
    SessionToken t1 = mock(SessionToken.class);
    SessionToken t2 = mock(SessionToken.class);
    MultipleSessionToken multi = new MultipleSessionToken(t1, t2);

    // strategy() should only include t1
    when(t1.findToken(ctx)).thenReturn("found");
    when(t2.findToken(ctx)).thenReturn(null);

    multi.saveToken(ctx, "sid");

    verify(t1).saveToken(ctx, "sid");
    verify(t2, never()).saveToken(ctx, "sid");
  }

  @Test
  @DisplayName("Verify deleteToken propagates to all strategies if no existing token is found")
  void testDeleteToken_NoneFound() {
    Context ctx = mock(Context.class);
    SessionToken t1 = mock(SessionToken.class);
    SessionToken t2 = mock(SessionToken.class);
    MultipleSessionToken multi = new MultipleSessionToken(t1, t2);

    when(t1.findToken(ctx)).thenReturn(null);
    when(t2.findToken(ctx)).thenReturn(null);

    multi.deleteToken(ctx, "sid");

    verify(t1).deleteToken(ctx, "sid");
    verify(t2).deleteToken(ctx, "sid");
  }

  @Test
  @DisplayName("Verify deleteToken only removes from strategies where a token already exists")
  void testDeleteToken_SomeFound() {
    Context ctx = mock(Context.class);
    SessionToken t1 = mock(SessionToken.class);
    SessionToken t2 = mock(SessionToken.class);
    MultipleSessionToken multi = new MultipleSessionToken(t1, t2);

    // strategy() should only include t1
    when(t1.findToken(ctx)).thenReturn("found");
    when(t2.findToken(ctx)).thenReturn(null);

    multi.deleteToken(ctx, "sid");

    verify(t1).deleteToken(ctx, "sid");
    verify(t2, never()).deleteToken(ctx, "sid");
  }
}
