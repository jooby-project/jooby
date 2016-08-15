package scanner;

import org.jooby.mvc.GET;
import org.jooby.mvc.Path;

@Path("/")
public class MyController {

  @GET
  public String index() {
    return "It works!";
  }
}
