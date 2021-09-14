package tests.i2405;

import io.jooby.annotations.GET;
import io.jooby.annotations.QueryParam;

public class C2405 {
  @GET("/2405/blah")
  public String blah(@QueryParam Bean2405 blah) {
    return blah.toString();
  }

  @GET("/2405/blah2")
  public String blah2(@QueryParam Bean2405 blah) {
    return blah.toString();
  }
}
