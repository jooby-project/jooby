package apps;

import org.jooby.Request;
import org.jooby.Response;
import org.jooby.mvc.POST;
import org.jooby.mvc.Path;

@Path("/1096")
public class Route1096d {
  @POST
  @Path("/mvc/form")
  public String myApi(Request req, Response res, Form1096 form){
    return form.toString();
  }
}