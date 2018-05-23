package apps;

import org.jooby.Request;
import org.jooby.Response;
import org.jooby.mvc.GET;
import org.jooby.mvc.Path;

@Path("/")
public class Route1096 {
  @GET
  public String myApi(Request req, Response res, Param1096 params){
    return params.toString();
  }
}