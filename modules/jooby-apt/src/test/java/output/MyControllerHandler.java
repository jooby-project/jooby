package output;

import io.jooby.Context;
import io.jooby.Reified;
import io.jooby.Route;
import io.jooby.Value;
import kotlin.coroutines.Continuation;
import source.SuspendRoute;

import javax.annotation.Nonnull;
import javax.inject.Provider;

public class MyControllerHandler implements Route.Handler {

  private Provider<MyController> provider;

  public MyControllerHandler(Provider<MyController> provider) {
    this.provider = provider;
  }

  @Nonnull @Override public Object apply(@Nonnull Context ctx) throws Exception {
    return provider.get().doIt(param(ctx.path(), "p1").to(String.class));
  }

  private static Value param(Value scope, String name) {
    Value value = scope.get(name);
    return value.isMissing() && scope.size() > 0 ? scope : value;
  }
}
