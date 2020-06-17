package issues.i1795;

import io.jooby.annotations.POST;
import io.swagger.v3.oas.annotations.parameters.RequestBody;

import java.util.List;

public class Controller1795 {
  @POST("/param")
  public List<String> create(@RequestBody(required = true, description = "String list") List<String> list) {
    return list;
  }

  @POST("/method")
  @RequestBody(description = "At method level list")
  public List<String> createAtMethod(List<String> list) {
    return list;
  }
}
