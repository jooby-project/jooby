package output;

import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.MvcFactory;

import javax.annotation.Nonnull;
import javax.inject.Provider;

public class MvcExtension implements MvcFactory {

  private static void install(Jooby application, Provider<MyController> provider) throws Exception {
    application.get("/mypath", ctx -> {
      MyController myController = provider.get();
      return myController.controllerMethod();
    }).setReturnType(String.class)
        .attribute("RoleAnnotation", "User");
  }

  @Override public boolean supports(@Nonnull Class type) {
    return type == MyController.class;
  }

  @Nonnull @Override public Extension create(@Nonnull Provider provider) {
    return app -> install(app, provider);
  }
}
