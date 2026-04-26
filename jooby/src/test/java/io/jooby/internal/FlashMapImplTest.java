/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.jooby.Context;
import io.jooby.Cookie;
import io.jooby.value.Value;
import io.jooby.value.ValueFactory;

public class FlashMapImplTest {

  private Context ctx;
  private Cookie template;

  @BeforeEach
  void setUp() {
    ctx = mock(Context.class);
    template = new Cookie("jooby.flash");
  }

  @Test
  void testInitWithMissingCookie() {
    when(ctx.cookie("jooby.flash")).thenReturn(Value.missing(new ValueFactory(), "jooby.flash"));
    FlashMapImpl flash = new FlashMapImpl(ctx, template);
    assertTrue(flash.isEmpty());
    verify(ctx, never()).setResponseCookie(any());
  }

  @Test
  void testInitWithExistingCookie() {
    // Mock existing cookie with data: success=true
    Value cookieValue = mock(Value.class);
    when(cookieValue.isMissing()).thenReturn(false);
    when(cookieValue.value()).thenReturn("success=true");
    when(ctx.cookie("jooby.flash")).thenReturn(cookieValue);

    FlashMapImpl flash = new FlashMapImpl(ctx, template);

    assertEquals("true", flash.get("success"));
    // Initial sync should set maxAge=0 to discard after reading
    ArgumentCaptor<Cookie> captor = ArgumentCaptor.forClass(Cookie.class);
    verify(ctx).setResponseCookie(captor.capture());
    assertEquals(0, captor.getValue().getMaxAge());
  }

  @Test
  void testKeepLogic() {
    when(ctx.cookie("jooby.flash")).thenReturn(Value.missing(new ValueFactory(), "jooby.flash"));
    FlashMapImpl flash = new FlashMapImpl(ctx, template);

    // Keep on empty flash does nothing
    flash.keep();
    verify(ctx, never()).setResponseCookie(any());

    // Keep with data sets cookie
    flash.put("foo", "bar");
    reset(ctx);
    flash.keep();

    ArgumentCaptor<Cookie> captor = ArgumentCaptor.forClass(Cookie.class);
    verify(ctx).setResponseCookie(captor.capture());
    assertTrue(captor.getValue().getValue().contains("foo=bar"));
  }

  @Test
  void testToCookieBranches() {
    // 1. Existing data detection (Initial Scope > 0)
    Value cookieValue = mock(Value.class);
    when(cookieValue.isMissing()).thenReturn(false);
    when(cookieValue.value()).thenReturn("a=b");
    when(ctx.cookie("jooby.flash")).thenReturn(cookieValue);
    FlashMapImpl flash = new FlashMapImpl(ctx, template);

    // Branch 1.a: No change detected, existing data -> MaxAge(0)
    reset(ctx);
    flash.put("a", "b"); // same as initial
    // sync triggered by put
    ArgumentCaptor<Cookie> captor = ArgumentCaptor.forClass(Cookie.class);
    verify(ctx).setResponseCookie(captor.capture());
    assertEquals(0, captor.getValue().getMaxAge());

    // Branch 2.a: Change detected, size 0 -> MaxAge(0)
    reset(ctx);
    flash.remove("a");
    verify(ctx).setResponseCookie(captor.capture());
    assertEquals(0, captor.getValue().getMaxAge());

    // Branch 2.b: Change detected, size > 0 -> Set Value
    reset(ctx);
    flash.put("new", "val");
    verify(ctx).setResponseCookie(captor.capture());
    assertTrue(captor.getValue().getValue().contains("new=val"));
  }

  @Test
  void testAllMutationMethods() {
    when(ctx.cookie("jooby.flash")).thenReturn(Value.missing(new ValueFactory(), "jooby.flash"));
    FlashMapImpl flash = new FlashMapImpl(ctx, template);

    // put
    flash.put("k", "v");
    // putAll
    flash.putAll(Map.of("k2", "v2"));
    // putIfAbsent
    flash.putIfAbsent("k3", "v3");
    // compute
    flash.compute("k", (k, v) -> "v_new");
    // computeIfAbsent
    flash.computeIfAbsent("k4", k -> "v4");
    // computeIfPresent
    flash.computeIfPresent("k4", (k, v) -> "v4_new");
    // merge
    flash.merge("k2", "merged", (v1, v2) -> v2);
    // replace(k, v)
    flash.replace("k3", "v3_new");
    // replace(k, old, new)
    flash.replace("k3", "v3_new", "v3_final");
    // replaceAll
    flash.replaceAll((k, v) -> "all");
    // remove(k)
    flash.remove("k");
    // remove(k, v)
    flash.remove("k2", "all");

    verify(ctx, times(12)).setResponseCookie(any());
  }
}
