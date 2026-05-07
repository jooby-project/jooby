/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.htmx;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jooby.Context;
import io.jooby.json.JsonEncoder;
import io.jooby.value.Value;

class HtmxContextTest {

  private Context ctx;
  private HtmxContext htmx;

  @BeforeEach
  void setUp() {
    ctx = mock(Context.class);
    htmx = new HtmxContext(ctx);
  }

  // --- Reader Tests ---

  @Test
  void shouldReadBooleanHeadersWhenTrue() {
    mockHeader("HX-Boosted", true);
    mockHeader("HX-Request", true);
    mockHeader("HX-History-Restore-Request", true);

    assertTrue(htmx.isBoosted());
    assertTrue(htmx.isHtmxRequest());
    assertTrue(htmx.isHistoryRestoreRequest());
  }

  @Test
  void shouldReadBooleanHeadersWhenFalseOrMissing() {
    mockHeader("HX-Boosted", false);
    mockHeader("HX-Request", false);
    mockHeader("HX-History-Restore-Request", false);

    assertFalse(htmx.isBoosted());
    assertFalse(htmx.isHtmxRequest());
    assertFalse(htmx.isHistoryRestoreRequest());
  }

  @Test
  void shouldReadStringHeaders() {
    Value urlValue = mock(Value.class);
    when(urlValue.valueOrNull()).thenReturn("https://jooby.io");
    when(ctx.header("HX-Current-Url")).thenReturn(urlValue);

    Value targetValue = mock(Value.class);
    when(targetValue.valueOrNull()).thenReturn("#main-div");
    when(ctx.header("HX-Target")).thenReturn(targetValue);

    assertEquals("https://jooby.io", htmx.getCurrentUrl());
    assertEquals("#main-div", htmx.getTarget());
  }

  @Test
  void shouldReturnNullForMissingStringHeaders() {
    Value missingValue = mock(Value.class);
    when(missingValue.valueOrNull()).thenReturn(null);
    when(ctx.header("HX-Current-Url")).thenReturn(missingValue);
    when(ctx.header("HX-Target")).thenReturn(missingValue);

    assertNull(htmx.getCurrentUrl());
    assertNull(htmx.getTarget());
  }

  // --- Modifier Tests ---

  @Test
  void shouldSetModifierHeaders() {
    assertSame(htmx, htmx.pushUrl("/new-url"));
    verify(ctx).setResponseHeader("HX-Push-Url", "/new-url");

    assertSame(htmx, htmx.replaceUrl("/replace-url"));
    verify(ctx).setResponseHeader("HX-Replace-Url", "/replace-url");

    assertSame(htmx, htmx.redirect("/redirect"));
    verify(ctx).setResponseHeader("HX-Redirect", "/redirect");

    assertSame(htmx, htmx.refresh());
    verify(ctx).setResponseHeader("HX-Refresh", true);

    assertSame(htmx, htmx.reswap("outerHTML"));
    verify(ctx).setResponseHeader("HX-Reswap", "outerHTML");

    assertSame(htmx, htmx.retarget("#error-box"));
    verify(ctx).setResponseHeader("HX-Retarget", "#error-box");
  }

  // --- Trigger Tests (String Join Branch) ---

  @Test
  void shouldTriggerEventsWithoutPayloads() {
    htmx.trigger("event1");
    verify(ctx).setResponseHeader("HX-Trigger", "event1");

    // Add a second event to verify joining logic
    htmx.trigger("event2", null);
    verify(ctx).setResponseHeader("HX-Trigger", "event1, event2");

    htmx.triggerAfterSettle("settle1");
    verify(ctx).setResponseHeader("HX-Trigger-After-Settle", "settle1");

    htmx.triggerAfterSwap("swap1");
    verify(ctx).setResponseHeader("HX-Trigger-After-Swap", "swap1");
  }

  // --- Trigger Tests (JSON Encoder Branch) ---

  @Test
  void shouldTriggerEventsWithPayloads() {
    JsonEncoder encoder = mock(JsonEncoder.class);
    when(ctx.require(JsonEncoder.class)).thenReturn(encoder);

    // Simulate JSON encoding output
    when(encoder.encode(any())).thenReturn("{\"event1\":{\"key\":\"value\"}}");

    Map<String, String> payload = Map.of("key", "value");

    // HX-Trigger
    htmx.trigger("event1", payload);
    verify(encoder, times(1)).encode(any());
    verify(ctx).setResponseHeader("HX-Trigger", "{\"event1\":{\"key\":\"value\"}}");

    // HX-Trigger-After-Settle
    htmx.triggerAfterSettle("event1", payload);
    verify(encoder, times(2)).encode(any());
    verify(ctx).setResponseHeader("HX-Trigger-After-Settle", "{\"event1\":{\"key\":\"value\"}}");

    // HX-Trigger-After-Swap
    htmx.triggerAfterSwap("event1", payload);
    verify(encoder, times(3)).encode(any());
    verify(ctx).setResponseHeader("HX-Trigger-After-Swap", "{\"event1\":{\"key\":\"value\"}}");
  }

  // --- Defensive Branch Coverage ---

  @Test
  void shouldSafelyIgnoreEmptyMapInUpdateTriggerHeader() throws Exception {
    // Uses reflection to hit the defensive `if (triggerMap.isEmpty()) return;`
    // which is practically unreachable via public methods since .put() happens first.
    Method updateMethod =
        HtmxContext.class.getDeclaredMethod("updateTriggerHeader", String.class, Map.class);
    updateMethod.setAccessible(true);

    // Invoke with empty map
    updateMethod.invoke(htmx, "HX-Trigger", Collections.emptyMap());

    // Verify context was never touched
    verify(ctx, never()).setResponseHeader(anyString(), anyString());
    verify(ctx, never()).require(JsonEncoder.class);
  }

  // --- Helper Methods ---

  private void mockHeader(String headerName, boolean value) {
    Value val = mock(Value.class);
    when(val.booleanValue(false)).thenReturn(value);
    when(ctx.header(headerName)).thenReturn(val);
  }
}
