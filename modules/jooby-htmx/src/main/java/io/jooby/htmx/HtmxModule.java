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
 * The primary extension for enabling first-class HTMX support in a Jooby application.
 *
 * <p>Installing this module registers the {@link HtmxTemplateEngine}, which intercepts {@code
 * HtmxModelAndView} responses and enables advanced features like Out-Of-Band (OOB) template
 * swapping and declarative HTTP headers.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * {
 * // Basic installation
 * install(new HtmxModule());
 *
 * // Installation with a global HTMX error handler
 * install(new HtmxModule(new MyHtmxErrorHandler()));
 * }
 * }</pre>
 */
public class HtmxModule implements Extension {

  private @Nullable HtmxErrorHandler errorHandler;

  /**
   * Creates a new HTMX module with a custom global error handler.
   *
   * @param errorHandler The handler to process and format exceptions into HTMX-compatible
   *     responses.
   */
  public HtmxModule(HtmxErrorHandler errorHandler) {
    this.errorHandler = errorHandler;
  }

  /** Creates a new HTMX module with default settings. */
  public HtmxModule() {}

  /**
   * Installs the HTMX extension into the Jooby application.
   *
   * @param app The target Jooby application.
   * @throws Exception If an error occurs during installation.
   */
  @Override
  public void install(Jooby app) throws Exception {

    if (errorHandler != null) {
      app.error(errorHandler.toErrorHandler());
    }

    app.encoder(new HtmxTemplateEngine());
  }
}
