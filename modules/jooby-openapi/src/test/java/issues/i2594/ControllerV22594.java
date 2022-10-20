/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i2594;

import io.jooby.annotations.GET;
import io.jooby.annotations.Path;
import jakarta.inject.Inject;

@Path("/")
public class ControllerV22594 {

  @Inject private WelcomeService2594 welcomeService;

  @GET("/welcome")
  public String sayHi() {
    return welcomeService.welcome("v2");
  }
}
