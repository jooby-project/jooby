package output;

import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.MvcFactory;
import source.Controller1527;

import edu.umd.cs.findbugs.annotations.NonNull;
import jakarta.inject.Provider;

public class MvcExtension implements MvcFactory {

  private static void install(Jooby application, Provider<MyController> provider) throws Exception {
    application.get("/mypath", ctx -> {
      MyController myController = provider.get();
      myController.controllerMethod();
      return ctx;
    }).attribute("RequireRole", Controller1527.Role.USER);
  }

  @Override public boolean supports(@NonNull Class type) {
    return type == MyController.class;
  }

  @NonNull @Override public Extension create(@NonNull Provider provider) {
    return app -> install(app, provider);
  }
}
