package issues.i1573;

import io.jooby.annotations.GET;
import io.jooby.annotations.Path;
import io.jooby.annotations.PathParam;

import java.util.Optional;

@Path("/c")
public class Controller1573 {

  @GET("/profile/{id}?")
  public String profile(@PathParam Optional<String> id) {
    return id.orElse("self");
  }
}
