package tests.i1814;

import io.jooby.Extension;
import io.jooby.MvcFactory;
import io.jooby.Route;

import javax.annotation.Nonnull;
import javax.inject.Provider;

public class Expected1814 implements MvcFactory {

  @Override public boolean supports(@Nonnull Class type) {
    return false;
  }

  @Nonnull @Override public Extension create(@Nonnull Provider provider) {
    return application -> {
      Route route = application.get("/1814", ctx -> {
        C1814 controller = (C1814) provider.get();
        String type = ctx.query("type").value();
        return controller.getUsers(type, ctx.getRoute());
      });
      route.setReturnType(U1814.class);
    };
  }

}
