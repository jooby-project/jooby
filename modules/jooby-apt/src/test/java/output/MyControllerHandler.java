package output;

import io.jooby.Context;
import io.jooby.Route;
import io.jooby.Value;

import javax.annotation.Nonnull;
import javax.inject.Provider;

public class MyControllerHandler implements Route.Handler {

  private Provider<MyController> provider;

  public MyControllerHandler(Provider<MyController> provider) {
    this.provider = provider;
  }

  @Nonnull @Override public Object apply(@Nonnull Context ctx) throws Exception {
    return provider.get().doIt(x(ctx.query("x")));
  }

  private static Integer x(Value x) {
    return x.to(Integer.class);
  }
}
