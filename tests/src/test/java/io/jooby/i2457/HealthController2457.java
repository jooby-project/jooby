package io.jooby.i2457;

import jakarta.inject.Inject;

import com.google.common.collect.ImmutableMap;
import io.jooby.Context;
import io.jooby.annotations.GET;
import io.jooby.annotations.Path;

@Path("/")
public class HealthController2457 {

  @Inject
  private WelcomeService2457 welcomeService;

  @GET("/healthcheck")
  public void healthCheck(Context ctx) {
    String welcome = welcomeService.welcome("healthcheck");
    ctx.setResponseCode(200)
        .render(ImmutableMap.of(
            "status", "Ok",
            "welcome", welcome)
        );
  }
}
