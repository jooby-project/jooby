package output;

import io.jooby.Context;
import io.jooby.Route;
import io.jooby.Value;
import kotlin.coroutines.Continuation;
import source.SuspendRoute;

import javax.annotation.Nonnull;
import javax.inject.Provider;

public class MyControllerHandler implements Route.Handler {

  private Provider<SuspendRoute> provider;

  public MyControllerHandler(Provider<SuspendRoute> provider) {
    this.provider = provider;
  }

  @Nonnull @Override public Object apply(@Nonnull Context ctx) throws Exception {
    return provider.get().suspendFun((Continuation) ctx.getAttributes().remove("__continuation"));
  }

  private static Integer x(Value x) {
    return x.to(Integer.class);
  }
}
