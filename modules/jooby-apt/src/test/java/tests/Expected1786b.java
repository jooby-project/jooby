package tests;

import io.jooby.Extension;
import io.jooby.MvcFactory;
import io.jooby.exception.MissingValueException;
import source.Controller1786b;

import edu.umd.cs.findbugs.annotations.NonNull;
import jakarta.inject.Provider;
import java.util.UUID;

public class Expected1786b implements MvcFactory {

  @Override public boolean supports(@NonNull Class type) {
    return false;
  }

  @NonNull @Override public Extension create(@NonNull Provider provider) {
    return application -> {
      application.get("/required-param", ctx -> {
        Controller1786b controller = (Controller1786b) provider.get();
        UUID uuid = ctx.query("uuid").to(UUID.class);
        return controller.requiredParam(MissingValueException.requireNonNull("uuid", uuid));
      });
    };
  }

}
