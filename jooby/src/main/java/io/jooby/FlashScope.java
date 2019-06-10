package io.jooby;

import javax.annotation.Nonnull;

import static io.jooby.FlashMap.NAME;
import static java.util.Objects.requireNonNull;

public class FlashScope implements Extension {

  private final Cookie template;

  public FlashScope(@Nonnull Cookie template) {
    this.template = requireNonNull(template, "Flash cookie is required.");
  }

  public FlashScope() {
    this(cookie());
  }

  @Override public void install(@Nonnull Jooby application) {
    application.before(ctx -> ctx.attribute(NAME, FlashMap.create(ctx, template)));
  }

  public static Cookie cookie() {
    return new Cookie("jooby.flash").setHttpOnly(true);
  }
}
