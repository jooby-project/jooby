package jooby;

import jooby.mvc.Body;
import jooby.mvc.GET;
import jooby.mvc.POST;
import jooby.mvc.Path;

import com.google.common.collect.ImmutableMap;

@Path("/resource")
public class Resource {

  @GET
  public Object index(final String name) {
    return ImmutableMap.builder()
        .put("name", name)
        .build();
  }

  public @POST Object user(final @Body User user) {
    return user;
  }
}
