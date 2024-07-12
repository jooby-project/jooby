/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i2457;

import com.google.common.collect.ImmutableMap;
import io.jooby.Context;
import io.jooby.annotation.GET;
import io.jooby.annotation.Path;

@Path("/")
public class HealthController2457 {

  private WelcomeService2457 welcomeService;

  @jakarta.inject.Inject
  public HealthController2457(WelcomeService2457 welcomeService) {
    this.welcomeService = welcomeService;
  }

  @GET("/healthcheck")
  public void healthCheck(Context ctx) {
    String welcome = welcomeService.welcome("healthcheck");
    ctx.setResponseCode(200).render(ImmutableMap.of("status", "Ok", "welcome", welcome));
  }
}
