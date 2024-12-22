/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jte;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import gg.jte.TemplateEngine;
import gg.jte.TemplateOutput;
import io.jooby.Context;
import io.jooby.MapModelAndView;
import io.jooby.ModelAndView;
import io.jooby.buffer.DataBuffer;
import io.jooby.buffer.DataBufferFactory;

public class Issue3599 {

  @Test
  public void shouldNotCallObjectMethodOnMapModels() {
    var bufferFactory = mock(DataBufferFactory.class);
    var buffer = mock(DataBuffer.class);
    when(bufferFactory.allocateBuffer()).thenReturn(buffer);

    var attributes = Map.<String, Object>of("foo", 1);
    var mapModel = new HashMap<String, Object>();
    mapModel.put("param1", new Issue3599());

    var ctx = mock(Context.class);
    when(ctx.getBufferFactory()).thenReturn(bufferFactory);
    when(ctx.getAttributes()).thenReturn(attributes);

    var jteParams = new HashMap<String, Object>();
    jteParams.putAll(attributes);
    jteParams.putAll(mapModel);

    var jte = mock(TemplateEngine.class);

    var engine = new JteTemplateEngine(jte);
    var modelAndView = new MapModelAndView("template.jte", mapModel);
    engine.render(ctx, modelAndView);

    verify(jte, times(1)).render(eq("template.jte"), eq(jteParams), isA(TemplateOutput.class));
  }

  @Test
  public void shouldCallObjectMethodOnObjectModels() {
    var bufferFactory = mock(DataBufferFactory.class);
    var buffer = mock(DataBuffer.class);
    when(bufferFactory.allocateBuffer()).thenReturn(buffer);

    var attributes = Map.<String, Object>of("foo", 1);
    var model = new Issue3599();

    var ctx = mock(Context.class);
    when(ctx.getBufferFactory()).thenReturn(bufferFactory);
    when(ctx.getAttributes()).thenReturn(attributes);

    var jte = mock(TemplateEngine.class);

    var engine = new JteTemplateEngine(jte);
    var modelAndView = new ModelAndView<>("template.jte", model);
    engine.render(ctx, modelAndView);

    verify(jte, times(1)).render(eq("template.jte"), eq(model), isA(TemplateOutput.class));
  }
}
