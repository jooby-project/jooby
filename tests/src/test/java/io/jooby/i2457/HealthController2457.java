package io.jooby.i2457;


import com.google.common.collect.ImmutableMap;

import io.jooby.annotations.GET;
import io.jooby.annotations.Path;
import io.jooby.Context;

@Path("/")
public class HealthController2457 {

  private WelcomeService2457 welcomeService;

  @javax.inject.Inject //Guice does not support jakarta annotations
  public HealthController2457(WelcomeService2457 welcomeService) {
    super();
    this.welcomeService = welcomeService;
  }

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
