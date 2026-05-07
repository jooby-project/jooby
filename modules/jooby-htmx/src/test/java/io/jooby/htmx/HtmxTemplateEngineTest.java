/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.htmx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jooby.Context;
import io.jooby.Jooby;
import io.jooby.ModelAndView;
import io.jooby.Router;
import io.jooby.TemplateEngine;
import io.jooby.output.BufferedOutput;
import io.jooby.output.Output;
import io.jooby.output.OutputFactory;

class HtmxTemplateEngineTest {

  private HtmxTemplateEngine engine;
  private Context ctx;
  private Router router;
  private Jooby app;

  @BeforeEach
  void setUp() {
    engine = new HtmxTemplateEngine();
    ctx = mock(Context.class);
    router = mock(Router.class);
    app = mock(Jooby.class);

    when(ctx.getRouter()).thenReturn(router);
    when(app.getRouter()).thenReturn(router);
  }

  // --- Lifecycle / Init Tests ---

  @Test
  void shouldThrowIllegalStateExceptionWhenNoOtherTemplateEnginesRegistered() {
    // Router only has the HtmxTemplateEngine registered, no underlying engines like Handlebars
    when(router.getTemplateEngines()).thenReturn(List.of(engine));

    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> engine.init(app));

    assertEquals("No template engines registered", ex.getMessage());
  }

  // --- Supports Tests ---

  @Test
  void shouldSupportHtmxModelAndView() {
    HtmxModelAndView<?> htmxView = mock(HtmxModelAndView.class);
    assertTrue(engine.supports(htmxView));
  }

  @Test
  void shouldNotSupportStandardModelAndView() {
    ModelAndView<?> standardView = ModelAndView.of("view.hbs", null);
    assertFalse(engine.supports(standardView));
  }

  // --- Render Tests ---

  @Test
  void shouldReturnNullForStandardModelAndView() throws Exception {
    ModelAndView<?> standardView = ModelAndView.of("view.hbs", null);
    assertNull(engine.render(ctx, standardView));
  }

  @Test
  void shouldThrowIllegalStateExceptionWhenNoTemplateEngineFound() {
    // 1. Setup incompatible engine and initialize the HtmxTemplateEngine
    TemplateEngine incompatibleEngine = mock(TemplateEngine.class);
    when(router.getTemplateEngines()).thenReturn(Arrays.asList(engine, incompatibleEngine));
    engine.init(app); // Cache the engines

    // 2. Setup the HTMX view
    HtmxModelAndView<?> htmxView = mock(HtmxModelAndView.class);
    when(htmxView.getView()).thenReturn("missing.hbs");
    when(incompatibleEngine.supports(htmxView)).thenReturn(false);

    // 3. Execute and verify
    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> engine.render(ctx, htmxView));

    assertEquals("No template engine registered to handle: missing.hbs", ex.getMessage());
  }

  @Test
  void shouldRenderMultipleTemplatesIntoCompositeOutput() throws Exception {
    // 1. Mock the HtmxModelAndView and its iterator (simulating multiple OOB views)
    HtmxModelAndView<?> htmxView = mock(HtmxModelAndView.class);
    ModelAndView primaryView = ModelAndView.of("main.hbs", null);
    ModelAndView oobView = ModelAndView.of("oob.hbs", null);
    List views = Arrays.asList(primaryView, oobView);

    when(htmxView.iterator()).thenReturn(views.iterator());

    // 2. Mock Delegate Engines
    TemplateEngine delegateEngine = mock(TemplateEngine.class);
    when(delegateEngine.supports(htmxView)).thenReturn(true);

    TemplateEngine incompatibleEngine = mock(TemplateEngine.class);
    when(incompatibleEngine.supports(htmxView)).thenReturn(false);

    // Register and initialize engines (HtmxTemplateEngine should remove itself from the cached
    // list)
    when(router.getTemplateEngines())
        .thenReturn(Arrays.asList(engine, incompatibleEngine, delegateEngine));
    engine.init(app);

    // 3. Mock the Output Pipeline
    OutputFactory outputFactory = mock(OutputFactory.class);
    when(ctx.getOutputFactory()).thenReturn(outputFactory);

    BufferedOutput composite = mock(BufferedOutput.class);
    when(outputFactory.newComposite()).thenReturn(composite);

    // Primary View Output
    Output primaryOutput = mock(Output.class);
    ByteBuffer primaryBuffer = ByteBuffer.wrap("primary".getBytes());
    when(primaryOutput.asByteBuffer()).thenReturn(primaryBuffer);
    when(delegateEngine.encode(ctx, primaryView)).thenReturn(primaryOutput);

    // OOB View Output
    Output oobOutput = mock(Output.class);
    ByteBuffer oobBuffer = ByteBuffer.wrap("oob".getBytes());
    when(oobOutput.asByteBuffer()).thenReturn(oobBuffer);
    when(delegateEngine.encode(ctx, oobView)).thenReturn(oobOutput);

    // 4. Execute
    Output result = engine.render(ctx, htmxView);

    // 5. Verify
    assertSame(composite, result, "Should return the composite output builder");

    // Verify that the byte buffers were written to the composite sequentially
    verify(composite).write(primaryBuffer);
    verify(composite).write(oobBuffer);
  }
}
