/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jte;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.junit.jupiter.api.Test;

import gg.jte.TemplateOutput;
import gg.jte.models.runtime.JteModel;
import io.jooby.Context;
import io.jooby.internal.jte.JteModelEncoder;
import io.jooby.output.BufferedOutput;
import io.jooby.output.BufferedOutputFactory;

public class Issue3602 {

  @Test
  public void shouldRenderJteModel() throws Exception {
    var bufferFactory = mock(BufferedOutputFactory.class);
    var buffer = mock(BufferedOutput.class);
    when(bufferFactory.newBufferedOutput()).thenReturn(buffer);

    var attributes = Map.<String, Object>of("foo", 1);
    var ctx = mock(Context.class);
    when(ctx.getOutputFactory()).thenReturn(bufferFactory);
    when(ctx.getAttributes()).thenReturn(attributes);

    var model = mock(JteModel.class);

    var engine = new JteModelEncoder();
    engine.encode(ctx, model);

    verify(model, times(1)).render(isA(TemplateOutput.class));
  }
}
