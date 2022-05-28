package issues.i2594;

import javax.inject.Inject;

import io.jooby.annotations.GET;
import io.jooby.annotations.Path;

@Path("/")
public class ControllerV12594 {

  @Inject
  private WelcomeService2594 welcomeService;

  @GET("/welcome")
  public String sayHi() {
    return welcomeService.welcome("v1");
  }

  @GET("/should-not-be-duplicated-under-v2")
  public String demo() {
    return welcomeService.welcome("v1");
  }
}
