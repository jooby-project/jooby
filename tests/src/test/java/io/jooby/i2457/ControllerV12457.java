package io.jooby.i2457;

import jakarta.inject.Inject;

import io.jooby.annotations.GET;
import io.jooby.annotations.Path;

@Path("/")
public class ControllerV12457 {

  @Inject
  private WelcomeService2457 welcomeService;

  @GET("/welcome")
  public String sayHi() {
    return welcomeService.welcome("v1");
  }
}
