/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.htmx;

import org.jspecify.annotations.Nullable;

import io.jooby.Extension;
import io.jooby.Jooby;

/**
 * Module for HTMX support.
 *
 * <p>Installing this module enables:
 *
 * <ul>
 *   <li>Sequential template streaming for Out-of-Band (OOB) swaps via {@code @HxOob}.
 *   <li>Native dependency injection of {@link HtmxContext} into MVC controllers.
 * </ul>
 *
 * <h2>Usage:</h2>
 *
 * <pre>{@code
 * {
 *   install(new HtmxModule());
 * }
 * }</pre>
 */
public class HtmxModule implements Extension {

  private @Nullable HtmxErrorHandler errorHandler;

  public HtmxModule(HtmxErrorHandler errorHandler) {
    this.errorHandler = errorHandler;
  }

  public HtmxModule() {}

  @Override
  public void install(Jooby app) throws Exception {

    if (errorHandler != null) {
      app.error(errorHandler.toErrorHandler());
    }

    app.encoder(new HtmxTemplateEngine());
  }
}
