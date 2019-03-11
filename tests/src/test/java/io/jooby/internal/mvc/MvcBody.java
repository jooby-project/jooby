package io.jooby.internal.mvc;

import io.jooby.annotations.POST;
import io.jooby.annotations.Path;
import io.jooby.annotations.QueryParam;

import java.util.Map;

public class MvcBody {
  @POST
  @Path(("/body/str"))
  public String bodyString(String body) {
    return String.valueOf(body);
  }

  @POST
  @Path(("/body/int"))
  public int bodyInt(int body) {
    return body;
  }

  @POST
  @Path(("/body/json"))
  public Object bodyInt(Map<String, Object> body, @QueryParam String type) {
    return body + type;
  }
}
