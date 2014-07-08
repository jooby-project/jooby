package jooby;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.inject.Named;

import jooby.mvc.Body;
import jooby.mvc.GET;
import jooby.mvc.Header;
import jooby.mvc.POST;
import jooby.mvc.Path;

import com.google.common.collect.ImmutableMap;

@Path("/resource")
public class Resource {

  @GET
  @Path("/optional")
  public Object index(final Optional<String> value, @Named("JSESSIONID") final Cookie sessionId) {
    return ImmutableMap.builder()
        .put("value", value)
        .put("JSESSIONID", sessionId)
        .build();
  }

  @GET
  @Path("/vars/{id}")
  public Object index(final String id) {
    return ImmutableMap.builder()
        .put("value", id)
        .build();
  }

  @GET
  @Path("/string")
  public Object index(final String value, @Header("user-agent") final String userAgent) {
    return ImmutableMap.builder()
        .put("value", value)
        .put("userAgent", userAgent)
        .build();
  }

  @GET
  @Path("/int-array")
  public Object index(final int[] value) {
    return ImmutableMap.builder()
        .put("value", Arrays.toString(value))
        .build();
  }

  @GET
  @Path("/list-of-strings")
  public Object index(final List<String> value) {
    return ImmutableMap.builder()
        .put("value", value)
        .build();
  }

  public @POST Object user(final @Body User user) {
    return user;
  }
}
