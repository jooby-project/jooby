package io.jooby;

import javax.annotation.Nonnull;

public interface Filter {
  @Nonnull Handler apply(@Nonnull Handler next);

  @Nonnull default Filter then(@Nonnull Filter next) {
    return h -> apply(next.apply(h));
  }

  @Nonnull default Handler then(@Nonnull Handler next) {
    return ctx -> apply(next).apply(ctx);
  }

}
