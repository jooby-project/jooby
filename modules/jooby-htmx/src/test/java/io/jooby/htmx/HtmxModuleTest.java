/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.htmx;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jooby.ErrorHandler;
import io.jooby.Jooby;

class HtmxModuleTest {

  private Jooby app;

  @BeforeEach
  void setUp() {
    app = mock(Jooby.class);
  }

  @Test
  void shouldInstallWithoutErrorHandler() throws Exception {
    HtmxModule module = new HtmxModule();
    module.install(app);

    // Verify error handler was NOT registered
    verify(app, never()).error(any(ErrorHandler.class));

    // Verify the template engine WAS registered
    verify(app).encoder(any(HtmxTemplateEngine.class));

    // Verify the init lifecycle hook was registered
    verify(app).onStarting(any());
  }

  @Test
  void shouldInstallWithErrorHandler() throws Exception {
    // 1. Mock the HTMX Error Handler and its conversion method
    HtmxErrorHandler htmxErrorHandler = mock(HtmxErrorHandler.class);
    ErrorHandler joobyErrorHandler = mock(ErrorHandler.class);
    when(htmxErrorHandler.toErrorHandler()).thenReturn(joobyErrorHandler);

    // 2. Initialize and install the module
    HtmxModule module = new HtmxModule(htmxErrorHandler);
    module.install(app);

    // 3. Verify the converted error handler WAS registered
    verify(app).error(joobyErrorHandler);

    // 4. Verify the template engine WAS registered
    verify(app).encoder(any(HtmxTemplateEngine.class));

    // 5. Verify the init lifecycle hook was registered
    verify(app).onStarting(any());
  }
}
