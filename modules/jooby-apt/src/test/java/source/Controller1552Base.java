package source;

import io.jooby.Context;
import io.jooby.annotations.GET;
import io.jooby.annotations.POST;

import java.util.Arrays;
import java.util.List;

public abstract class Controller1552Base {

  @GET
  public String doIt(Context ctx) {
    return ctx.getRequestPath();
  }

  @GET("/subpath")
  public List<String> subpath(Context ctx) {
    return Arrays.asList(ctx.getRequestPath());
  }

  @GET("/object")
  public Object object(Context ctx) {
    return ctx;
  }

  @POST("/post")
  public JavaBeanParam post(Context ctx) {
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

  @POST(path = "/postOnly")
  public String pathPostOnly(Context ctx) {
    return ctx.getRequestPath();
  }
}
