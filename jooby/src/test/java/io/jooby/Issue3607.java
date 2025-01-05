/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import io.jooby.buffer.DataBuffer;
import io.jooby.buffer.DefaultDataBufferFactory;

public class Issue3607 {

  private static class TemplateEngineImpl implements TemplateEngine {
    @Override
    public DataBuffer render(Context ctx, ModelAndView<?> modelAndView) throws Exception {
      // do nothing
      return DefaultDataBufferFactory.sharedInstance.wrap(new byte[0]);
    }
  }

  @Test
  public void shouldNotGenerateEmptyFlashMap() throws Exception {
    var ctx = mock(Context.class);

    var templateEngine = new TemplateEngineImpl();
    templateEngine.encode(ctx, ModelAndView.map("index.html"));

    verify(ctx, times(1)).flashOrNull();
    verify(ctx, times(1)).sessionOrNull();
    verify(ctx, times(1)).setDefaultResponseType(MediaType.html);
  }
}
