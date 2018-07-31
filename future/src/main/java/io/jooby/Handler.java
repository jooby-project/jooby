package io.jooby;

import javax.annotation.Nonnull;

public interface Handler {

  Handler NOT_FOUND = ctx -> {
    throw new Err(StatusCode.NOT_FOUND);
  };

  Handler FAVICON = ctx -> {
    ctx.sendStatusCode(StatusCode.NOT_FOUND);
    return ctx;
  };

  @Nonnull Object apply(@Nonnull Context ctx) throws Exception;

}
