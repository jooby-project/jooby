/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i2594;

import com.google.common.collect.ImmutableMap;
import io.jooby.Context;
import io.jooby.annotations.GET;
import io.jooby.annotations.Path;
import jakarta.inject.Inject;

@Path("/")
public class HealthController2594 {

  @Inject private WelcomeService2594 welcomeService;

  @GET("/healthcheck")
  public void healthCheck(Context ctx) {
    String welcome = welcomeService.welcome("healthcheck");
    ctx.setResponseCode(200).render(ImmutableMap.of("status", "Ok", "welcome", welcome));
  }
}
