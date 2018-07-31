package io.jooby;

import javax.annotation.Nonnull;

public interface RootHandler {
  void apply(@Nonnull Context ctx);
}
