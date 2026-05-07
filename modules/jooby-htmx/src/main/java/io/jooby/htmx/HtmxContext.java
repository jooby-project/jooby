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

/**
 * Provides a fluent API for interacting with HTMX specific HTTP headers. * This context wraps a
 * standard Jooby {@link Context} and makes it easy to read incoming HTMX request states and safely
 * build outgoing HTMX response headers, including complex JSON-encoded trigger payloads.
 *
 * @see <a href="https://htmx.org/reference/#headers">HTMX Reference</a>
 * @author edgar
 * @since 4.5.0
 */
public class HtmxContext {

  private final Context ctx;

  private final Map<String, @Nullable Object> triggers = new LinkedHashMap<>();
  private final Map<String, @Nullable Object> triggersAfterSettle = new LinkedHashMap<>();
  private final Map<String, @Nullable Object> triggersAfterSwap = new LinkedHashMap<>();

  /**
   * Creates a new HTMX context.
   *
   * @param ctx The current Jooby HTTP context.
   */
  public HtmxContext(Context ctx) {
    this.ctx = ctx;
  }

  // --- Request State Readers ---

  /**
   * Indicates that the request is via an element using hx-boost.
   *
   * @return True if the {@code HX-Boosted} header is present and true.
   */
  public boolean isBoosted() {
    return ctx.header("HX-Boosted").booleanValue(false);
  }

  /**
   * Indicates that the request is a standard HTMX request.
   *
   * @return True if the {@code HX-Request} header is present and true.
   */
  public boolean isHtmxRequest() {
    return ctx.header("HX-Request").booleanValue(false);
  }

  /**
   * Indicates if the request is for history restoration after a miss in the local history cache.
   *
   * @return True if the {@code HX-History-Restore-Request} header is present and true.
   */
  public boolean isHistoryRestoreRequest() {
    return ctx.header("HX-History-Restore-Request").booleanValue(false);
  }

  /**
   * Retrieves the current URL of the browser that made the HTMX request.
   *
   * @return The value of the {@code HX-Current-Url} header, or null if missing.
   */
  public @Nullable String getCurrentUrl() {
    return ctx.header("HX-Current-Url").valueOrNull();
  }

  /**
   * Retrieves the id of the target element if it exists.
   *
   * @return The value of the {@code HX-Target} header, or null if missing.
   */
  public @Nullable String getTarget() {
    return ctx.header("HX-Target").valueOrNull();
  }

  // --- Response Header Modifiers ---

  /**
   * Pushes a new url into the history stack.
   *
   * @param url The URL to push into the history stack.
   * @return This context for method chaining.
   */
  public HtmxContext pushUrl(String url) {
    ctx.setResponseHeader("HX-Push-Url", url);
    return this;
  }

  /**
   * Replaces the current URL in the location bar.
   *
   * @param url The URL to replace in the location bar.
   * @return This context for method chaining.
   */
  public HtmxContext replaceUrl(String url) {
    ctx.setResponseHeader("HX-Replace-Url", url);
    return this;
  }

  /**
   * Forces a client-side redirect to a new location.
   *
   * @param url The target URL for the redirect.
   * @return This context for method chaining.
   */
  public HtmxContext redirect(String url) {
    ctx.setResponseHeader("HX-Redirect", url);
    return this;
  }

  /**
   * Instructs the client side to do a full refresh of the page.
   *
   * @return This context for method chaining.
   */
  public HtmxContext refresh() {
    ctx.setResponseHeader("HX-Refresh", true);
    return this;
  }

  /**
   * Specifies how the response will be swapped, overriding the default behavior.
   *
   * @param swap The swap strategy (e.g., innerHTML, outerHTML, beforebegin).
   * @return This context for method chaining.
   */
  public HtmxContext reswap(String swap) {
    ctx.setResponseHeader("HX-Reswap", swap);
    return this;
  }

  /**
   * Updates the target of the content update to a different element on the page.
   *
   * @param target A CSS selector representing the new target element.
   * @return This context for method chaining.
   */
  public HtmxContext retarget(String target) {
    ctx.setResponseHeader("HX-Retarget", target);
    return this;
  }

  // --- Trigger Builders (Object Payloads) ---

  /**
   * Triggers a client-side event as soon as the response is received.
   *
   * @param eventName The name of the event to trigger.
   * @return This context for method chaining.
   */
  public HtmxContext trigger(String eventName) {
    this.triggers.put(eventName, null);
    updateTriggerHeader("HX-Trigger", triggers);
    return this;
  }

  /**
   * Triggers a client-side event with an attached data payload.
   *
   * @param eventName The name of the event to trigger.
   * @param payload The data object to send with the event.
   * @return This context for method chaining.
   */
  public HtmxContext trigger(String eventName, @Nullable Object payload) {
    this.triggers.put(eventName, payload);
    updateTriggerHeader("HX-Trigger", triggers);
    return this;
  }

  /**
   * Triggers a client-side event after the settling step.
   *
   * @param eventName The name of the event to trigger.
   * @return This context for method chaining.
   */
  public HtmxContext triggerAfterSettle(String eventName) {
    this.triggersAfterSettle.put(eventName, null);
    updateTriggerHeader("HX-Trigger-After-Settle", triggersAfterSettle);
    return this;
  }

  /**
   * Triggers a client-side event with a payload after the settling step.
   *
   * @param eventName The name of the event to trigger.
   * @param payload The data object to send with the event.
   * @return This context for method chaining.
   */
  public HtmxContext triggerAfterSettle(String eventName, @Nullable Object payload) {
    this.triggersAfterSettle.put(eventName, payload);
    updateTriggerHeader("HX-Trigger-After-Settle", triggersAfterSettle);
    return this;
  }

  /**
   * Triggers a client-side event after the swap step.
   *
   * @param eventName The name of the event to trigger.
   * @return This context for method chaining.
   */
  public HtmxContext triggerAfterSwap(String eventName) {
    this.triggersAfterSwap.put(eventName, null);
    updateTriggerHeader("HX-Trigger-After-Swap", triggersAfterSwap);
    return this;
  }

  /**
   * Triggers a client-side event with a payload after the swap step.
   *
   * @param eventName The name of the event to trigger.
   * @param payload The data object to send with the event.
   * @return This context for method chaining.
   */
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
