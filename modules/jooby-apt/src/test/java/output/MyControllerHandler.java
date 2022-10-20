/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package output;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.Route;
import io.jooby.StatusCode;
import jakarta.inject.Provider;

public class MyControllerHandler implements Route.Handler {

  private Provider<MyController> provider;

  public MyControllerHandler(Provider<MyController> provider) {
    this.provider = provider;
  }

  @NonNull @Override
  public Object apply(@NonNull Context ctx) throws Exception {
    StatusCode statusCode = provider.get().controllerMethod();
    ctx.setResponseCode(statusCode);
    return statusCode;
  }
}
