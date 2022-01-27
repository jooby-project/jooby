package issues.i2505;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.jooby.annotations.GET;
import io.jooby.annotations.Path;

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
