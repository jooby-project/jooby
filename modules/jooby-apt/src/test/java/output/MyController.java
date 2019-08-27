package output;

import io.jooby.annotations.GET;
import io.jooby.annotations.Path;

import java.util.Map;

@Path("/")
public class MyController {

  @GET
  public String doIt(Map<String, Object> mybean) {
    return mybean.toString();
  }
}
