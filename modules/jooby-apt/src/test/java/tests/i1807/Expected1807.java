package tests.i1807;

import io.jooby.Extension;
import io.jooby.MvcFactory;
import io.jooby.exception.MissingValueException;
import source.Controller1786b;

import edu.umd.cs.findbugs.annotations.NonNull;
import jakarta.inject.Provider;
import java.util.UUID;

public class Expected1807 implements MvcFactory {

  @Override public boolean supports(@NonNull Class type) {
    return false;
  }

  @NonNull @Override public Extension create(@NonNull Provider provider) {
    return application -> {
      application.get("/test/{word}", ctx -> {
        C1807 controller = (C1807) provider.get();
        Word1807 word = ctx.multipart().to(Word1807.class);
        controller.hello(MissingValueException.requireNonNull("data", word));
        return ctx;
      });
    };
  }

}
