package source;

import io.jooby.annotations.GET;
import io.jooby.annotations.Path;
import kotlin.coroutines.Continuation;

@Path("/suspend")
public class SuspendRoute {

  @GET
  public Continuation suspendFun(Continuation continuation) {
    return continuation;
  }
}
