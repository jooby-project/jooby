package output;

import io.jooby.Context;
import io.jooby.Reified;
import io.jooby.Route;
import io.jooby.ValueNode;

import javax.annotation.Nonnull;
import javax.inject.Provider;
import java.lang.reflect.Type;
import java.util.Map;

public class MyControllerHandler implements Route.Handler {

  private Provider<MyController> provider;

  public MyControllerHandler(Provider<MyController> provider) {
    this.provider = provider;
  }

  @Nonnull @Override public Object apply(@Nonnull Context ctx) throws Exception {
    return provider.get().doIt(attriubute(ctx,"foo"));
  }

  private static <T> T attriubute(Context ctx, String name) {
    T value = ctx.attribute(name);
    if (value == null) {
      return (T) ctx.getAttributes();
    }
    return value;
  }
}
