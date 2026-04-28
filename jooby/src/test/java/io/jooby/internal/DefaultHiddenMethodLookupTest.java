/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jooby.Context;
import io.jooby.Router;
import io.jooby.value.Value;

public class DefaultHiddenMethodLookupTest {

  @Test
  @DisplayName("Verify lookup is skipped for non-POST requests")
  void testIgnoreNonPost() {
    Context ctx = mock(Context.class);
    when(ctx.getMethod()).thenReturn(Router.GET);

    DefaultHiddenMethodLookup lookup = new DefaultHiddenMethodLookup("_method");
    Optional<String> result = lookup.apply(ctx);

    assertTrue(result.isEmpty());

    verify(ctx).getMethod();
    verifyNoMoreInteractions(ctx);
  }

  @Test
  @DisplayName("Verify successful lookup from form data during POST")
  void testPostWithHiddenMethod() {
    Context ctx = mock(Context.class);
    Value formValue = mock(Value.class);

    when(ctx.getMethod()).thenReturn(Router.POST);
    when(ctx.form("_method")).thenReturn(formValue);
    when(formValue.toOptional()).thenReturn(Optional.of("PUT"));

    DefaultHiddenMethodLookup lookup = new DefaultHiddenMethodLookup("_method");
    Optional<String> result = lookup.apply(ctx);

    assertTrue(result.isPresent());
    assertEquals("PUT", result.get());
  }

  @Test
  @DisplayName("Verify lookup returns empty if parameter is missing during POST")
  void testPostWithoutHiddenMethod() {
    Context ctx = mock(Context.class);
    Value formValue = mock(Value.class);

    when(ctx.getMethod()).thenReturn(Router.POST);
    when(ctx.form("_method")).thenReturn(formValue);
    when(formValue.toOptional()).thenReturn(Optional.empty());

    DefaultHiddenMethodLookup lookup = new DefaultHiddenMethodLookup("_method");
    Optional<String> result = lookup.apply(ctx);

    assertTrue(result.isEmpty());
  }
}
