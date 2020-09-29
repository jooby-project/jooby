package tests.i2026;

import javax.inject.Provider;

import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.MvcFactory;
import output.MyController;

public class Expected2026 implements MvcFactory {

  private static void install(Jooby application, Provider<C2026> provider) throws Exception {
    application.get("/api/todo", ctx -> {
      C2026 myController = provider.get();
      return myController.handle();
    });
  }

  @Override public boolean supports(Class type) {
    return type == MyController.class;
  }

  @Override public Extension create(Provider provider) {
    return app -> install(app, provider);
  }
}
