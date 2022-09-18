package source;

import io.jooby.annotations.GET;
import io.jooby.annotations.QueryParam;

import edu.umd.cs.findbugs.annotations.NonNull;

public class Controller1786 {

  @GET("/required-string-param")
  public String requiredStringParam(@QueryParam @NonNull String value) {
    return value;
  }
}
