package issues.i2594;

import jakarta.inject.Inject;

import io.jooby.annotations.GET;
import io.jooby.annotations.Path;

@Path("/")
public class ControllerV22594 {

  @Inject
  private WelcomeService2594 welcomeService;

  @GET("/welcome")
  public String sayHi() {
    return welcomeService.welcome("v2");
  }
}
