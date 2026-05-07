/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.htmx;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import io.jooby.Context;
import io.jooby.json.JsonEncoder;

public class HtmxContext {

  private final Context ctx;

  // Notice the value type is now Object!
  private final Map<String, @Nullable Object> triggers = new LinkedHashMap<>();
  private final Map<String, @Nullable Object> triggersAfterSettle = new LinkedHashMap<>();
  private final Map<String, @Nullable Object> triggersAfterSwap = new LinkedHashMap<>();

  public HtmxContext(Context ctx) {
    this.ctx = ctx;
  }

  // --- Request State Readers ---

  /** Indicates that the request is via an element using hx-boost. */
  public boolean isBoosted() {
    return Boolean.parseBoolean(ctx.header("HX-Boosted").value("false"));
  }

  /** Indicates that the request is a standard HTMX request. */
  public boolean isHtmxRequest() {
    return Boolean.parseBoolean(ctx.header("HX-Request").value("false"));
  }

  /** True if the request is for history restoration after a miss in the local history cache. */
  public boolean isHistoryRestoreRequest() {
    return Boolean.parseBoolean(ctx.header("HX-History-Restore-Request").value("false"));
  }

  /** The current URL of the browser. */
  public @Nullable String getCurrentUrl() {
    return ctx.header("HX-Current-Url").valueOrNull();
  }

  /** The id of the target element if it exists. */
  public @Nullable String getTarget() {
    return ctx.header("HX-Target").valueOrNull();
  }

  // --- Response Header Modifiers ---

  /** Pushes a new url into the history stack. */
  public HtmxContext pushUrl(String url) {
    ctx.setResponseHeader("HX-Push-Url", url);
    return this;
  }

  /** Replaces the current URL in the location bar. */
  public HtmxContext replaceUrl(String url) {
    ctx.setResponseHeader("HX-Replace-Url", url);
    return this;
  }

  /** Can be used to do a client-side redirect to a new location. */
  public HtmxContext redirect(String url) {
    ctx.setResponseHeader("HX-Redirect", url);
    return this;
  }

  /** If set to true the client side will do a full refresh of the page. */
  public HtmxContext refresh() {
    ctx.setResponseHeader("HX-Refresh", "true");
    return this;
  }

  /** Allows you to specify how the response will be swapped. */
  public HtmxContext reswap(String swap) {
    ctx.setResponseHeader("HX-Reswap", swap);
    return this;
  }

  /**
   * A CSS selector that updates the target of the content update to a different element on the
   * page.
   */
  public HtmxContext retarget(String target) {
    ctx.setResponseHeader("HX-Retarget", target);
    return this;
  }

  // ... [Request readers and simple header setters remain the same] ...

  // --- Trigger Builders (Object Payloads) ---

  public HtmxContext trigger(String eventName) {
    this.triggers.put(eventName, null);
    updateTriggerHeader("HX-Trigger", triggers);
    return this;
  }

  public HtmxContext trigger(String eventName, @Nullable Object payload) {
    this.triggers.put(eventName, payload);
    updateTriggerHeader("HX-Trigger", triggers);
    return this;
  }

  public HtmxContext triggerAfterSettle(String eventName) {
    this.triggersAfterSettle.put(eventName, null);
    updateTriggerHeader("HX-Trigger-After-Settle", triggersAfterSettle);
    return this;
  }

  public HtmxContext triggerAfterSettle(String eventName, @Nullable Object payload) {
    this.triggersAfterSettle.put(eventName, payload);
    updateTriggerHeader("HX-Trigger-After-Settle", triggersAfterSettle);
    return this;
  }

  public HtmxContext triggerAfterSwap(String eventName) {
    this.triggersAfterSwap.put(eventName, null);
    updateTriggerHeader("HX-Trigger-After-Swap", triggersAfterSwap);
    return this;
  }

  public HtmxContext triggerAfterSwap(String eventName, @Nullable Object payload) {
    this.triggersAfterSwap.put(eventName, payload);
    updateTriggerHeader("HX-Trigger-After-Swap", triggersAfterSwap);
    return this;
  }

  // --- Safe JSON Encoding ---

  private void updateTriggerHeader(String headerName, Map<String, @Nullable Object> triggerMap) {
    if (triggerMap.isEmpty()) return;

    boolean hasPayloads = triggerMap.values().stream().anyMatch(Objects::nonNull);

    if (!hasPayloads) {
      // No objects to serialize, safe to use simple comma separation
      ctx.setResponseHeader(headerName, String.join(", ", triggerMap.keySet()));
    } else {
      var encoder = ctx.require(JsonEncoder.class);
      ctx.setResponseHeader(headerName, encoder.encode(triggerMap));
    }
  }
}
