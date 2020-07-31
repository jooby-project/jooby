package tests.i1859;

import io.jooby.Extension;
import io.jooby.MvcFactory;
import io.jooby.Route;

import javax.annotation.Nonnull;
import javax.inject.Provider;

public class Expected1859 implements MvcFactory {

  @Override public boolean supports(@Nonnull Class type) {
    return false;
  }

  @Nonnull @Override public Extension create(@Nonnull Provider provider) {
    return application -> {
      Route route = application.get("/c/1859", ctx -> {
        C1859 controller = (C1859) provider.get();
        String value = ctx.body().valueOrNull();
        return controller.foo(value);
      });
      route.setReturnType(String.class);
    };
  }

}
