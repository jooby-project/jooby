package source;

import io.jooby.annotations.ContextParam;
import io.jooby.annotations.GET;
import io.jooby.annotations.Path;
import io.jooby.annotations.SessionParam;

import java.util.Map;

@Path("/1387")
public class Issue1387 {

  public static class Data1387 {}

  @GET
  public String attribute(@ContextParam String userId) {
    return userId;
  }

  @GET("/primitive")
  public int attribute(@ContextParam int userId) {
    return userId;
  }

  @GET("/complex")
  public Data1387 attributeComplex(@ContextParam Data1387 data) {
    return data;
  }

  @GET("/attributes")
  public Map<String, Object> attributes(@ContextParam Map<String, Object> attributes) {
    return attributes;
  }

  @GET("/session")
  public String session(@SessionParam String userId) {
    return userId;
  }

  @GET("/session/int")
  public int session(@SessionParam int userId) {
    return userId;
  }
}
