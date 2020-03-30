/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.whoops;

import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.MediaType;
import io.jooby.internal.whoops.Whoops;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.nio.file.Paths;

import static io.jooby.ErrorHandler.errorMessage;

/**
 * Pretty page error handler for application exceptions. Usage:
 *
 * <pre>{@code
 * {
 *
 *   install(new WhoopsModule());
 *
 * }
 * }</pre>
 *
 * The whoops error handler shows pretty error page as long as we are able to:
 *
 * - Request accepts a text/html response or the accept header is missing.
 *
 *
 * @author edgar
 * @since 2.8.0
 */
public class WhoopsModule implements Extension {
  private Path basedir;

  public WhoopsModule(@Nonnull Path basedir) {
    this.basedir = basedir;
  }

  public WhoopsModule() {
    this(Paths.get(System.getProperty("user.dir")));
  }

  @Override public void install(@Nonnull Jooby application) {
    Whoops whoops = new Whoops(basedir);

    application.assets("/whoops/*", "/io/jooby/whoops");

    application.error((ctx, cause, code) -> {
      if (ctx.accept(MediaType.html)) {
        whoops.render(ctx, cause, code).handle((html, failure) -> {
          if (failure == null) {
            // Handle the exception
            application.getLog().error(errorMessage(ctx, code), cause);
            ctx.setResponseType(MediaType.html)
                .setResponseCode(code)
                .send(html);
          } else {
            application.getLog().error("whoops resulted in exception", failure);
          }
        });
      }
    });
  }
}
