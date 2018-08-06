package io.jooby;

import javax.annotation.Nonnull;

public interface After extends Filter {
  @Nonnull @Override default Handler apply(@Nonnull Handler next) {
    return ctx -> apply(ctx, next.apply(ctx));
  }

  @Nonnull Object apply(@Nonnull Context ctx, Object value) throws Exception;
}
