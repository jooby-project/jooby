package tests.i2325;

import io.jooby.annotations.GET;
import io.jooby.annotations.QueryParam;

public class C2325 {
  @GET("/2325")
  public String getMy(@QueryParam MyID2325 myId) {
    return myId.toString();
  }
}
