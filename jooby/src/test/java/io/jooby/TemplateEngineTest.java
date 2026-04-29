/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jooby.output.Output;

public class TemplateEngineTest {

  @Test
  @DisplayName("Verify encode initializes context and delegates to render")
  void testEncode() throws Exception {
    Context ctx = mock(Context.class);
    ModelAndView<?> mav = mock(ModelAndView.class);
    Output expectedOutput = mock(Output.class);

    // Create an anonymous implementation to test default methods
    TemplateEngine engine =
        new TemplateEngine() {
          @Override
          public Output render(Context ctx, ModelAndView<?> modelAndView) throws Exception {
            return expectedOutput;
          }
        };

    Output actualOutput = engine.encode(ctx, mav);

    assertSame(expectedOutput, actualOutput, "encode should return the output from render");

    // Verify side effects on Context
    verify(ctx).flashOrNull();
    verify(ctx).sessionOrNull();
    verify(ctx).setDefaultResponseType(MediaType.html);
  }

  @Test
  @DisplayName("Verify default extensions returns .html")
  void testDefaultExtensions() {
    TemplateEngine engine = (ctx, mav) -> null; // Minimal lambda implementation

    List<String> extensions = engine.extensions();
    assertEquals(1, extensions.size());
    assertEquals(".html", extensions.get(0));
  }

  @Test
  @DisplayName("Verify supports checks view name against all extensions")
  void testSupports() {
    // Implement an engine with multiple extensions to test loop branches
    TemplateEngine engine =
        new TemplateEngine() {
          @Override
          public Output render(Context ctx, ModelAndView<?> modelAndView) {
            return null;
          }

          @Override
          public List<String> extensions() {
            return Arrays.asList(".peb", ".html");
          }
        };

    ModelAndView<?> pebView = mock(ModelAndView.class);
    when(pebView.getView()).thenReturn("index.peb");

    ModelAndView<?> htmlView = mock(ModelAndView.class);
    when(htmlView.getView()).thenReturn("index.html");

    ModelAndView<?> txtView = mock(ModelAndView.class);
    when(txtView.getView()).thenReturn("index.txt");

    // Branch 1: Match on first extension
    assertTrue(engine.supports(pebView), "Should return true for .peb extension");

    // Branch 2: Match on subsequent extension
    assertTrue(engine.supports(htmlView), "Should return true for .html extension");

    // Branch 3: No match found, loop finishes
    assertFalse(engine.supports(txtView), "Should return false for unsupported extensions");
  }

  @Test
  @DisplayName("Verify normalizePath logic across all branches")
  void testNormalizePath() {
    // Branch 1: path is null
    assertNull(TemplateEngine.normalizePath(null));

    // Branch 2: path starts with a slash
    assertEquals("views", TemplateEngine.normalizePath("/views"));

    // Branch 3: path does NOT start with a slash
    assertEquals("views", TemplateEngine.normalizePath("views"));
    assertEquals("custom/path", TemplateEngine.normalizePath("custom/path"));
  }
}
