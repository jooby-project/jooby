package source;

import io.jooby.annotations.GET;
import io.jooby.annotations.QueryParam;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.UUID;

public class Controller1786b {

  @GET("/required-param")
  public UUID requiredParam(@QueryParam @NonNull UUID value) {
    return value;
  }
}
