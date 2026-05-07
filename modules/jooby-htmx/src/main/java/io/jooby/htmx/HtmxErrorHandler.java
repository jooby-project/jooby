/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.htmx;

import org.slf4j.event.Level;

import io.jooby.Context;
import io.jooby.ErrorHandler;
import io.jooby.StatusCode;

public interface HtmxErrorHandler {
  HtmxResponse apply(Context ctx, Throwable cause, StatusCode code);

  default ErrorHandler toErrorHandler() {
    return (ctx, cause, code) -> {
      // error is thrown on bad Htmx request, ignore we can't handle it.
      if (!(cause instanceof HtmxDirectAccessException)
          && ctx.header("HX-Request").booleanValue(false)) {
        var log = ctx.getRouter().getLog();
        var level = code.value() < 500 ? Level.DEBUG : Level.ERROR;
        log.atLevel(level).log(ErrorHandler.errorMessage(ctx, code), cause);
        ErrorHandler.errorMessage(ctx, code);
        apply(ctx, cause, code).send(ctx);
      }
    };
  }
}
