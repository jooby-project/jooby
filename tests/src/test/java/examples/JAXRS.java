package examples;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/jaxrs")
public class JAXRS {

  @GET
  public String getIt() {
    return "Got it!";
  }
}
