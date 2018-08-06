package io.jooby;

import javax.annotation.Nonnull;

public interface Before extends Filter {
  @Nonnull @Override default Handler apply(@Nonnull Handler next) {
    return ctx -> {
      before(ctx);
      return next.apply(ctx);
    };
  }

  void before(@Nonnull Context ctx) throws Exception;
}
