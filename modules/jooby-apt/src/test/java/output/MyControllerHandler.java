package output;

import io.jooby.Context;
import io.jooby.Route;
import io.jooby.ValueNode;

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

  private static ValueNode param(ValueNode scope, String name) {
    ValueNode value = scope.get(name);
    return value.isMissing() && scope.size() > 0 ? scope : value;
  }
}
