/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.htmx;

import static java.util.Optional.ofNullable;

import java.util.*;

import org.jspecify.annotations.Nullable;

import io.jooby.Context;
import io.jooby.StatusCode;
import io.jooby.json.JsonEncoder;

/**
 * An imperative builder for constructing HTMX responses safely and fluently.
 *
 * <p>This class allows developers to explicitly orchestrate complex HTMX interactions directly from
 * the controller, such as triggering client-side events, chaining Out-Of-Band (OOB) template swaps,
 * and managing HTTP status code behaviors (e.g., automatically upgrading a 204 No Content to a 200
 * OK if HTML views are attached).
 */
public class HtmxResponse {

  private final @Nullable String view;
  private final Object model;
  private @Nullable StatusCode status;

  private final Map<String, String> headers = new LinkedHashMap<>();

  private final Map<String, Object> oobs = new LinkedHashMap<>();
  private final Map<String, @Nullable Object> triggers = new LinkedHashMap<>();
  private final Map<String, @Nullable Object> triggersAfterSettle = new LinkedHashMap<>();
  private final Map<String, @Nullable Object> triggersAfterSwap = new LinkedHashMap<>();

  private HtmxResponse(@Nullable String view, Object model) {
    this.view = view;
    this.model = model;
  }

  /**
   * Creates an HtmxResponse that renders a specific view template with the provided model.
   *
   * @param view The classpath location of the template.
   * @param model The data model to pass to the template engine.
   * @return A new HtmxResponse instance.
   */
  public static HtmxResponse view(String view, @Nullable Object model) {
    return new HtmxResponse(view, ofNullable(model).orElse(Map.of()));
  }

  /**
   * Creates an HtmxResponse that renders a specific view template with an empty model.
   *
   * @param view The classpath location of the template.
   * @return A new HtmxResponse instance.
   */
  public static HtmxResponse view(String view) {
    return view(view, null);
  }

  /**
   * Creates an empty action-only response.
   *
   * <p>Defaults the HTTP status to {@link StatusCode#NO_CONTENT} (204). HTMX interprets a 204 as a
   * successful request but will not attempt to swap any content into the DOM.
   *
   * @return A new HtmxResponse instance.
   */
  public static HtmxResponse empty() {
    return empty(StatusCode.NO_CONTENT);
  }

  /**
   * Creates an empty action-only response with a specific status code.
   *
   * @param status The status code to return.
   * @return A new HtmxResponse instance.
   */
  public static HtmxResponse empty(StatusCode status) {
    var res = new HtmxResponse(null, Map.of());
    res.status = status;
    return res;
  }

  // --- Builder Methods ---

  /**
   * Sets the HTTP status code for the response.
   *
   * @param status The status code.
   * @return This builder instance.
   */
  public HtmxResponse status(StatusCode status) {
    this.status = status;
    return this;
  }

  /**
   * Triggers a client-side event immediately using the {@code HX-Trigger} header.
   *
   * @param eventName The name of the event to trigger.
   * @return This builder instance.
   */
  public HtmxResponse trigger(String eventName) {
    this.triggers.put(eventName, null);
    return this;
  }

  /**
   * Triggers a client-side event with a JSON payload immediately using the {@code HX-Trigger}
   * header.
   *
   * @param eventName The name of the event to trigger.
   * @param jsonPayload The event detail to be serialized into JSON.
   * @return This builder instance.
   */
  public HtmxResponse trigger(String eventName, Object jsonPayload) {
    this.triggers.put(eventName, jsonPayload);
    return this;
  }

  /**
   * Triggers a client-side event after the settling phase using {@code HX-Trigger-After-Settle}.
   *
   * @param eventName The name of the event to trigger.
   * @param value The event detail to be serialized into JSON, or null.
   * @return This builder instance.
   */
  public HtmxResponse triggerAfterSettle(String eventName, @Nullable Object value) {
    this.triggersAfterSettle.put(eventName, value);
    return this;
  }

  /**
   * Triggers a client-side event after the swap phase using {@code HX-Trigger-After-Swap}.
   *
   * @param eventName The name of the event to trigger.
   * @param value The event detail to be serialized into JSON, or null.
   * @return This builder instance.
   */
  public HtmxResponse triggerAfterSwap(String eventName, @Nullable Object value) {
    this.triggersAfterSwap.put(eventName, value);
    return this;
  }

  /**
   * Instructs HTMX to swap the response into a different target element. Sets the {@code
   * HX-Retarget} header.
   *
   * @param targetSelector The CSS selector of the new target.
   * @return This builder instance.
   */
  public HtmxResponse target(String targetSelector) {
    return header("HX-Retarget", targetSelector);
  }

  /**
   * Overrides the client-side swap logic for this specific response. Sets the {@code HX-Reswap}
   * header.
   *
   * @param swapStyle The swap style (e.g., "innerHTML", "outerHTML", "none").
   * @return This builder instance.
   */
  public HtmxResponse swap(String swapStyle) {
    return header("HX-Reswap", swapStyle);
  }

  /**
   * Pushes a new URL into the browser's history stack. Sets the {@code HX-Push-Url} header.
   *
   * @param url The URL to push. Use "false" to explicitly prevent history pushing.
   * @return This builder instance.
   */
  public HtmxResponse pushUrl(String url) {
    return header("HX-Push-Url", url);
  }

  /**
   * Forces the client to perform a full-page redirect to the specified URL. Sets the {@code
   * HX-Redirect} header.
   *
   * @param url The destination URL.
   * @return This builder instance.
   */
  public HtmxResponse redirect(String url) {
    return header("HX-Redirect", url);
  }

  /**
   * Forces the client to perform a full-page reload. Sets the {@code HX-Refresh: true} header.
   *
   * @return This builder instance.
   */
  public HtmxResponse refresh() {
    return header("HX-Refresh", "true");
  }

  /**
   * Adds a custom header to the HTMX response.
   *
   * @param name The header name.
   * @param value The header value.
   * @return This builder instance.
   */
  public HtmxResponse header(String name, String value) {
    this.headers.put(name, value);
    return this;
  }

  /**
   * Instructs HTMX to render an out-of-band (OOB) swap using the specified view template. The model
   * provided to the main response will be automatically shared with this OOB template.
   *
   * @param oobView The classpath location of the OOB template.
   * @return This builder instance.
   */
  public HtmxResponse addOob(String oobView) {
    return addOob(oobView, model);
  }

  /**
   * Adds an out-of-band (OOB) swap to this response, using the specified view template and
   * associated data model. The OOB swap allows rendering an HTML fragment outside the regular
   * content replacement target.
   *
   * @param oobView The classpath location of the OOB view template.
   * @param model The data model to associate with the OOB view template.
   * @return This builder instance.
   */
  public HtmxResponse addOob(String oobView, @Nullable Object model) {
    this.oobs.put(oobView, ofNullable(model).orElse(Map.of()));
    return this;
  }

  /**
   * Sends the HTTP response based on the configuration of the current HtmxResponse instance. If a
   * view is set, it returns a rendered {@code HtmxModelAndView} containing the view and model.
   * Otherwise, it sends the HTTP status directly through the provided context. Headers are written
   * to the context before forming the response.
   *
   * @param ctx The HTTP {@code Context} object representing the current request and response
   *     context.
   * @return The HTTP context object.
   */
  public Context send(Context ctx) {
    writeHeaders(ctx);
    var hasViews = view != null || !oobs.isEmpty();
    if (status != null) {
      if (status == StatusCode.NO_CONTENT && hasViews) {
        // HTTP 204 cannot contain a body. Upgrade to 200 OK if we are sending HTML.
        ctx.setResponseCode(StatusCode.OK);
      } else {
        // Respect user's 422, 201, etc.
        ctx.setResponseCode(status);
      }
    }
    if (hasViews) {
      HtmxModelAndView<?> htmxView;
      if (view == null) {
        var oobIter = oobs.entrySet().iterator();
        var firstOob = oobIter.next();
        htmxView = new HtmxModelAndView<>(firstOob.getKey(), firstOob.getValue());

        while (oobIter.hasNext()) {
          var nextOob = oobIter.next();
          htmxView.addOob(nextOob.getKey(), nextOob.getValue());
        }
      } else {
        htmxView = new HtmxModelAndView<>(view, model);
        oobs.forEach(htmxView::addOob);
      }
      return ctx.render(htmxView);
    } else {
      return ctx.send(status != null ? status : StatusCode.NO_CONTENT);
    }
  }

  /**
   * Called internally to safely encode and write all headers directly to the Jooby Context.
   *
   * @param ctx The active request context.
   */
  private void writeHeaders(Context ctx) {
    // Write simple static headers
    headers.forEach(ctx::setResponseHeader);

    // Safely encode and write dynamic triggers
    writeTriggerMap(ctx, "HX-Trigger", triggers);
    writeTriggerMap(ctx, "HX-Trigger-After-Settle", triggersAfterSettle);
    writeTriggerMap(ctx, "HX-Trigger-After-Swap", triggersAfterSwap);
  }

  private void writeTriggerMap(
      Context ctx, String headerName, Map<String, @Nullable Object> triggerMap) {
    if (triggerMap.isEmpty()) return;

    boolean hasPayloads = triggerMap.values().stream().anyMatch(Objects::nonNull);

    if (!hasPayloads) {
      ctx.setResponseHeader(headerName, String.join(", ", triggerMap.keySet()));
    } else {
      var encoder = ctx.require(JsonEncoder.class);
      ctx.setResponseHeader(headerName, encoder.encode(triggerMap));
    }
  }
}
