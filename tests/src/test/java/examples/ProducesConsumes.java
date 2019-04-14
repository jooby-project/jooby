package examples;

import io.jooby.annotations.GET;
import io.jooby.annotations.Path;
import io.jooby.annotations.Produces;

@Path("/")
public class ProducesConsumes {

  @GET
  @Produces({"application/json", "application/xml"})
  public Message doMessage() {
    return new Message("MVC");
  }
}
