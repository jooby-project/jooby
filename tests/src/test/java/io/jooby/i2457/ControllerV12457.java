/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i2457;

import io.jooby.annotation.GET;
import io.jooby.annotation.Path;

@Path("/")
public class ControllerV12457 {

  private WelcomeService2457 welcomeService;

  @javax.inject.Inject // Guice does not support jakarta annotations
  public ControllerV12457(WelcomeService2457 welcomeService) {
    super();
    this.welcomeService = welcomeService;
  }

  @GET("/welcome")
  public String sayHi() {
    return welcomeService.welcome("v1");
  }
}
