package source;

import io.jooby.Context;
import io.jooby.Reified;
import io.jooby.annotations.GET;
import io.jooby.annotations.POST;
import io.jooby.annotations.Path;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Controller1552Base {

  @GET
  public String doIt(Context ctx) {
    assertEquals(String.class, ctx.getRoute().getReturnType());
    return ctx.getRequestPath();
  }

  @GET("/subpath")
  public List<String> subpath(Context ctx) {
    assertEquals(Reified.list(String.class).getType(), ctx.getRoute().getReturnType());
    return Arrays.asList(ctx.getRequestPath());
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

  @GET(path = "/pathAttributeWork")
  public String pathAttributeWork(Context ctx) {
    return ctx.getRequestPath();
  }

  @GET(path = "/path", value = "/value")
  public String pathvalue(Context ctx) {
    return ctx.getRequestPath();
  }

  @GET(value = {"/path1", "/path2"})
  public String pathvalueArray(Context ctx) {
    return ctx.getRequestPath();
  }

  @POST(path = {"/path1", "/path2"})
  public String pathArray(Context ctx) {
    return ctx.getRequestPath();
  }
}
