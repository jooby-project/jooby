package source;

import io.jooby.annotations.GET;

public class PrimitiveReturnType {
  @GET
  public int returnType() {
    return 0;
  }
}
