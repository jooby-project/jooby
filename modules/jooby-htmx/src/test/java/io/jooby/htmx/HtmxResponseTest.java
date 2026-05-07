/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.htmx;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.jooby.Context;
import io.jooby.StatusCode;
import io.jooby.json.JsonEncoder;

class HtmxResponseTest {

  private Context ctx;

  @BeforeEach
  void setUp() {
    ctx = mock(Context.class);
    when(ctx.render(any())).thenReturn(ctx);
    when(ctx.send(any(StatusCode.class))).thenReturn(ctx);
  }

  // --- Static Initializers ---

  @Test
  void shouldCreateViewResponse() {
    var response = HtmxResponse.view("main.hbs");
    response.send(ctx);

    // Status is null by default for pure view responses, falls back to Jooby's default 200
    verify(ctx, never()).setResponseCode(any());
    verify(ctx).render(any(HtmxModelAndView.class));
  }

  @Test
  void shouldCreateViewResponseWithModel() {
    var response = HtmxResponse.view("main.hbs", Map.of("key", "val"));
    response.send(ctx);

    ArgumentCaptor<HtmxModelAndView> captor = ArgumentCaptor.forClass(HtmxModelAndView.class);
    verify(ctx).render(captor.capture());

    assertEquals("main.hbs", captor.getValue().getView());
    assertEquals(Map.of("key", "val"), captor.getValue().getModel());
  }

  @Test
  void shouldCreateEmptyResponseAndSend204() {
    HtmxResponse.empty().send(ctx);
    verify(ctx).setResponseCode(StatusCode.NO_CONTENT); // status != null block
    verify(ctx).send(StatusCode.NO_CONTENT); // fallback send block
  }

  @Test
  void shouldHandleExplicitNullStatusForEmptyResponse() {
    HtmxResponse.empty(null).send(ctx);
    // If explicitly forced to null, it sends 204 fallback at the very end
    verify(ctx).send(StatusCode.NO_CONTENT);
  }

  // --- Builder Headers & Triggers ---

  @Test
  void shouldBuildAndWriteStaticHeaders() {
    HtmxResponse.empty()
        .target("#app")
        .swap("outerHTML")
        .pushUrl("/new-url")
        .redirect("/redirect")
        .refresh()
        .header("Custom-Header", "Value")
        .send(ctx);

    verify(ctx).setResponseHeader("HX-Retarget", "#app");
    verify(ctx).setResponseHeader("HX-Reswap", "outerHTML");
    verify(ctx).setResponseHeader("HX-Push-Url", "/new-url");
    verify(ctx).setResponseHeader("HX-Redirect", "/redirect");
    verify(ctx).setResponseHeader("HX-Refresh", "true");
    verify(ctx).setResponseHeader("Custom-Header", "Value");
  }

  @Test
  void shouldWriteTriggersWithoutPayloads() {
    HtmxResponse.empty()
        .trigger("event1")
        .triggerAfterSettle("event2", null)
        .triggerAfterSwap("event3", null)
        .send(ctx);

    verify(ctx).setResponseHeader("HX-Trigger", "event1");
    verify(ctx).setResponseHeader("HX-Trigger-After-Settle", "event2");
    verify(ctx).setResponseHeader("HX-Trigger-After-Swap", "event3");
  }

  @Test
  void shouldWriteTriggersWithJsonPayloads() {
    JsonEncoder encoder = mock(JsonEncoder.class);
    when(ctx.require(JsonEncoder.class)).thenReturn(encoder);
    when(encoder.encode(any())).thenReturn("{\"event\":{\"data\":1}}");

    Map<String, Object> payload = Map.of("data", 1);

    HtmxResponse.empty()
        .trigger("event1", payload)
        .triggerAfterSettle("event2", payload)
        .triggerAfterSwap("event3", payload)
        .send(ctx);

    verify(encoder, times(3)).encode(any());
    verify(ctx).setResponseHeader("HX-Trigger", "{\"event\":{\"data\":1}}");
    verify(ctx).setResponseHeader("HX-Trigger-After-Settle", "{\"event\":{\"data\":1}}");
    verify(ctx).setResponseHeader("HX-Trigger-After-Swap", "{\"event\":{\"data\":1}}");
  }

  // --- OOB and Status Code Intelligence ---

  @Test
  void shouldUpgrade204To200WhenSendingHtmlViews() {
    // We create an empty response (default 204), but then add a view.
    // It MUST upgrade to 200, because HTTP 204 strictly forbids body content!
    HtmxResponse.empty().addOob("toast.hbs").send(ctx);

    verify(ctx).setResponseCode(StatusCode.OK);
    verify(ctx).render(any(HtmxModelAndView.class));
  }

  @Test
  void shouldRespectExplicitCustomStatusCodeWhenSendingViews() {
    HtmxResponse.view("form.hbs")
        .status(StatusCode.UNPROCESSABLE_ENTITY) // 422 Validations failed
        .send(ctx);

    verify(ctx).setResponseCode(StatusCode.UNPROCESSABLE_ENTITY);
    verify(ctx).render(any(HtmxModelAndView.class));
  }

  @Test
  void shouldPromoteFirstOobToMainViewIfPrimaryViewIsNull() {
    // If no primary view exists, the first OOB added becomes the root view
    // so that HtmxModelAndView functions correctly.
    HtmxResponse.empty()
        .addOob("oob1.hbs", Map.of("id", 1))
        .addOob("oob2.hbs", null) // null model falls back to empty map
        .send(ctx);

    ArgumentCaptor<HtmxModelAndView> captor = ArgumentCaptor.forClass(HtmxModelAndView.class);
    verify(ctx).render(captor.capture());

    HtmxModelAndView rendered = captor.getValue();

    // First OOB was promoted
    assertEquals("oob1.hbs", rendered.getView());
    assertEquals(Map.of("id", 1), rendered.getModel());
  }

  @Test
  void shouldAppendOobsToPrimaryView() {
    Object parentModel = Map.of("parent", "data");

    HtmxResponse.view("main.hbs", parentModel)
        .addOob("oob1.hbs") // Should inherit parentModel
        .addOob("oob2.hbs", Map.of("child", "data")) // Should use custom model
        .send(ctx);

    ArgumentCaptor<HtmxModelAndView> captor = ArgumentCaptor.forClass(HtmxModelAndView.class);
    verify(ctx).render(captor.capture());

    HtmxModelAndView rendered = captor.getValue();
    assertEquals("main.hbs", rendered.getView());
  }
}
