package tests;

import io.jooby.Context;
import io.jooby.Extension;
import io.jooby.MvcFactory;
import io.jooby.exception.MissingValueException;
import source.Controller1786b;

import javax.annotation.Nonnull;
import javax.inject.Provider;
import java.util.UUID;

public class Expected1786b implements MvcFactory {

  @Override public boolean supports(@Nonnull Class type) {
    return false;
  }

  @Nonnull @Override public Extension create(@Nonnull Provider provider) {
    return application -> {
      application.get("/required-param", ctx -> {
        Controller1786b controller = (Controller1786b) provider.get();
        UUID uuid = ctx.query("uuid").to(UUID.class);
        return controller.requiredParam(MissingValueException.requireNonNull("uuid", uuid));
      });
    };
  }

}
