package source;

import io.jooby.Context;
import io.jooby.Reified;
import io.jooby.annotations.GET;
import io.jooby.annotations.POST;
import io.jooby.annotations.Path;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Path("/path")
public class Routes {

  @GET
  public String doIt(Context ctx) {
    assertEquals(String.class, ctx.getRoute().getReturnType());
    return ctx.pathString();
  }

  @GET("/subpath")
  public List<String> subpath(Context ctx) {
    assertEquals(Reified.list(String.class).getType(), ctx.getRoute().getReturnType());
    return Arrays.asList(ctx.pathString());
  }

  @GET("/object")
  public Object object(Context ctx) {
    assertEquals(Object.class, ctx.getRoute().getReturnType());
    return ctx;
  }

  @POST("/post")
  public JavaBeanParam post(Context ctx) {
    assertEquals(JavaBeanParam.class, ctx.getRoute().getReturnType());
    return new JavaBeanParam();
  }
}
