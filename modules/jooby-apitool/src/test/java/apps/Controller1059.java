package apps;

import org.jooby.mvc.GET;
import org.jooby.mvc.Path;

import javax.inject.Inject;

/**
 * Top level comment.
 */
@Path("/test")
public class Controller1059 {

  @Inject
  public Controller1059() {
  }

  /**
   * Say hi.
   * @return Hi.
   */
  @GET
  public String salute() {
    return "Hi";
  }

  /**
   * Say X.
   * @return Hi.
   */
  @GET
  @Path("/x")
  public String salutex() {
    return "Hi";
  }
}
