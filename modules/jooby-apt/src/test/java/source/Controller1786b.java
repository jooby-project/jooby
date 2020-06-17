package source;

import io.jooby.annotations.GET;
import io.jooby.annotations.QueryParam;

import javax.annotation.Nonnull;
import java.util.UUID;

public class Controller1786b {

  @GET("/required-param")
  public UUID requiredParam(@QueryParam @Nonnull UUID value) {
    return value;
  }
}
