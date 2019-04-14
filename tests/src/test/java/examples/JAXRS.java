package examples;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/jaxrs")
public class JAXRS {

  @GET
  public String getIt() {
    return "Got it!";
  }
}
