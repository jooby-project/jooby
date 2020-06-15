package source;

import io.jooby.annotations.GET;
import io.jooby.annotations.QueryParam;

import javax.annotation.Nonnull;

public class Controller1786 {

  @GET("/required-string-param")
  public String requiredStringParam(@QueryParam @Nonnull String value) {
    return value;
  }
}
