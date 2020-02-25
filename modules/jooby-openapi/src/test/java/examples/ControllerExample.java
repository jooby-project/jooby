package examples;

import io.jooby.Context;
import io.jooby.Session;
import io.jooby.annotations.GET;
import io.jooby.annotations.POST;
import io.jooby.annotations.Path;
import io.jooby.annotations.QueryParam;

import java.util.List;
import java.util.Optional;

@Path("/api")
public class ControllerExample {

  @GET({"/foo", "bar"})
  public String doSomething(@QueryParam Optional<String> q) {
    return "xxx";
  }

  @GET @POST("/post")
  public String twoMethods(@QueryParam boolean bool, @QueryParam short s, @QueryParam int i,
      @QueryParam char c, @QueryParam long l, @QueryParam float f, @QueryParam double d) {
    return "xxx";
  }

  @GET @Path(("/path"))
  public String pathAtMethodLevel(Context ctx) {
    return "xxx";
  }

  @Path("/path-only")
  public String pathOnly() {
    return "xxx";
  }

  @Path("/session")
  public String ifSession(Optional<Session> ifSession) {
    return "xxx";
  }

  @GET("/returnList")
  @Deprecated
  public List<String> returnList() {
    return null;
  }

  @POST("/bean")
  public ABean save(ABean bean) {
    return bean;
  }
}
