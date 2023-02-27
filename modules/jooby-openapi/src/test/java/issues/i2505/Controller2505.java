/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i2505;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.jooby.annotation.GET;
import io.jooby.annotation.Path;

public class Controller2505 {
  @GET
  @Path("/2505")
  public Map<String, String> mapWithStringValue() {
    return Collections.emptyMap();
  }

  @GET
  @Path("/2505/value")
  public Map<String, Value2505> mapWithCustomValue() {
    return Collections.emptyMap();
  }

  @GET
  @Path("/2505/arrayValue")
  public Map<String, List<Value2505>> mapWithCustomArrayValue() {
    return Collections.emptyMap();
  }
}
