package examples;

import io.jooby.annotations.Consumes;
import io.jooby.annotations.GET;
import io.jooby.annotations.Path;
import io.jooby.annotations.Produces;

public class ProducesConsumes {

  @Path("/produces")
  @GET
  @Produces({"application/json", "application/xml"})
  public Message produces() {
    return new Message("MVC");
  }

  @Path("/consumes")
  @GET
  @Consumes({"application/json", "application/xml"})
  public String consumes(Message body) {
    return body.toString();
  }
}
