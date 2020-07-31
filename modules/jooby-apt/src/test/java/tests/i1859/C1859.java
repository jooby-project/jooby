package tests.i1859;

import io.jooby.annotations.POST;
import io.jooby.annotations.Path;

import java.util.Optional;

@Path(("/c"))
public class C1859 {
  @POST("/i1859")
  public String foo(String theBodyParam) {
    return Optional.ofNullable(theBodyParam).orElse("empty");
  }
}
