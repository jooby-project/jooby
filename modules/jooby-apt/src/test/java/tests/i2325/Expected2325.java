package tests.i2325;

import jakarta.inject.Provider;

import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.MvcFactory;

public class Expected2325 implements MvcFactory {

  private static void install(Jooby application, Provider<C2325> provider) throws Exception {
    application.get("/api/todo", ctx -> {
      C2325 myController = provider.get();
      return myController.getMy(getXXX(ctx));
    });
  }

  private static MyID2325 getXXX(io.jooby.Context ctx) {
    return ctx.query("myId").isMissing()
        ? ctx.query().to(MyID2325.class)
        : ctx.query("myId").to(MyID2325.class);
  }

  @Override public boolean supports(Class type) {
    return type == C2325.class;
  }

  @Override public Extension create(Provider provider) {
    return app -> install(app, provider);
  }
}
