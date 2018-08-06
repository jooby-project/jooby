package io.jooby;

import javax.annotation.Nonnull;

public interface Handler {

  Handler NOT_FOUND = ctx -> {
    throw new Err(StatusCode.NOT_FOUND);
  };

  Handler METHOD_NOT_ALLOWED = ctx -> {
    throw new Err(StatusCode.METHOD_NOT_ALLOWED);
  };

  Handler FAVICON = ctx -> {
    ctx.sendStatusCode(StatusCode.NOT_FOUND);
    return ctx;
  };

  @Nonnull Object apply(@Nonnull Context ctx) throws Exception;

}
