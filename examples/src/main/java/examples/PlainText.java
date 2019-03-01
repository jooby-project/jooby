package examples;

import io.jooby.annotations.GET;
import io.jooby.annotations.Path;

public class PlainText {
  @GET
  @Path("/plaintext")
  public String plainText() {
    return "Hello, World!";
  }
}
