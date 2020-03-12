package output;

import io.jooby.Context;
import io.jooby.Reified;
import io.jooby.Route;
import io.jooby.StatusCode;
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
    ctx.setResponseCode(StatusCode.NO_CONTENT);
    provider.get().controllerMethod();
    return ctx;
  }
}
