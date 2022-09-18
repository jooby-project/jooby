package output;

import edu.umd.cs.findbugs.annotations.NonNull;
import jakarta.inject.Provider;

import io.jooby.Context;
import io.jooby.Route;
import io.jooby.StatusCode;

public class MyControllerHandler implements Route.Handler {

  private Provider<MyController> provider;

  public MyControllerHandler(Provider<MyController> provider) {
    this.provider = provider;
  }

  @NonNull @Override public Object apply(@NonNull Context ctx) throws Exception {
    StatusCode statusCode = provider.get().controllerMethod();
    ctx.setResponseCode(statusCode);
    return statusCode;
  }
}
