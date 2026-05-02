/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.thymeleaf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.IContext;

import io.jooby.Context;
import io.jooby.MapModelAndView;
import io.jooby.ModelAndView;
import io.jooby.output.BufferedOutput;
import io.jooby.output.Output;
import io.jooby.output.OutputFactory;

@ExtendWith(MockitoExtension.class)
class ThymeleafTemplateEngineTest {

  @Mock TemplateEngine templateEngine;
  @Mock Context ctx;
  @Mock OutputFactory outputFactory;
  @Mock BufferedOutput outputBuffer;
  @Mock Writer writer;

  private ThymeleafTemplateEngine engine;

  @BeforeEach
  void setup() {
    engine = new ThymeleafTemplateEngine(templateEngine, List.of(".thl", ".html"));
  }

  @Test
  void testExtensions_AreAssignedAndUnmodifiable() {
    assertEquals(List.of(".thl", ".html"), engine.extensions());
    assertThrows(UnsupportedOperationException.class, () -> engine.extensions().add(".bad"));
  }

  @Test
  void testRender_UnsupportedModelAndView() {
    ModelAndView<?> badModel = mock(ModelAndView.class);

    assertThrows(ModelAndView.UnsupportedModelAndView.class, () -> engine.render(ctx, badModel));
  }

  @Test
  void testRender_MapModelAndView_ViewWithoutSlash_LocaleFromContext() {
    MapModelAndView modelAndView = new MapModelAndView("index.html");
    modelAndView.put("user", "edgar");

    Map<String, Object> ctxAttributes = new HashMap<>();
    ctxAttributes.put("flash", "success");

    when(ctx.getAttributes()).thenReturn(ctxAttributes);
    when(ctx.locale()).thenReturn(Locale.UK);
    when(ctx.getOutputFactory()).thenReturn(outputFactory);
    when(outputFactory.allocate()).thenReturn(outputBuffer);
    when(outputBuffer.asWriter()).thenReturn(writer);

    Output result = engine.render(ctx, modelAndView);

    assertEquals(outputBuffer, result);

    ArgumentCaptor<IContext> contextCaptor = ArgumentCaptor.forClass(IContext.class);

    // Verifies the engine processed a sanitized path (prepended slash)
    verify(templateEngine).process(eq("/index.html"), contextCaptor.capture(), eq(writer));

    IContext thymeleafContext = contextCaptor.getValue();

    // Verifies the locale fell back to the context locale
    assertEquals(Locale.UK, thymeleafContext.getLocale());

    // Verifies model attributes and context attributes were successfully merged
    assertTrue(thymeleafContext.containsVariable("user"));
    assertEquals("edgar", thymeleafContext.getVariable("user"));
    assertTrue(thymeleafContext.containsVariable("flash"));
    assertEquals("success", thymeleafContext.getVariable("flash"));
  }

  @Test
  void testRender_MapModelAndView_ViewWithSlash_LocaleFromModel() {
    MapModelAndView modelAndView = new MapModelAndView("/admin/dashboard.html");
    modelAndView.setLocale(Locale.CANADA);

    when(ctx.getAttributes()).thenReturn(new HashMap<>());
    when(ctx.getOutputFactory()).thenReturn(outputFactory);
    when(outputFactory.allocate()).thenReturn(outputBuffer);
    when(outputBuffer.asWriter()).thenReturn(writer);

    engine.render(ctx, modelAndView);

    ArgumentCaptor<IContext> contextCaptor = ArgumentCaptor.forClass(IContext.class);

    // Verifies the engine processed the path as-is (slash already present)
    verify(templateEngine)
        .process(eq("/admin/dashboard.html"), contextCaptor.capture(), eq(writer));

    IContext thymeleafContext = contextCaptor.getValue();

    // Verifies the explicit model locale takes precedence
    assertEquals(Locale.CANADA, thymeleafContext.getLocale());
  }
}
