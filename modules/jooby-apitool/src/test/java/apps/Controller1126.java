package apps;

import org.jooby.mvc.Body;
import org.jooby.mvc.POST;
import org.jooby.mvc.Path;

@Path(("/1126"))
public class Controller1126 {

  public static class PingCommand {
    public String message;

    public int id;
  }

  @POST
  public Boolean doPing(@Body PingCommand pingCommand) {
    return pingCommand != null;
  }

  @POST
  @Path("/form")
  public Boolean doForm(PingCommand pingCommand) {
    return pingCommand != null;
  }
}
