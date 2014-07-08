package jooby;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;

import jooby.mvc.Body;
import jooby.mvc.GET;
import jooby.mvc.POST;
import jooby.mvc.Path;

import com.google.common.collect.ImmutableMap;

@Path("/resource")
public class Resource {

  private EntityManager em;

  @Inject
  public Resource(final EntityManager em) {
    this.em = requireNonNull(em, "The em is required.");
  }

  @GET
  @Path("/save")
  public Object index(final String id, final String firstName, final String lastName) {
    User user =  new User();
    user.setId(id);
    user.setFirstName(firstName);
    user.setLastName(lastName);
    em.persist(user);
    return user;
  }

  @GET
  @Path("/user")
  public Object index(final String id) {
    User user = em.find(User.class, id);
    return user;
  }

  @GET
  @Path("/optional")
  public Object index(final Optional<String> value, @Named("JSESSIONID") final Cookie sessionId) {
    return ImmutableMap.builder()
        .put("value", value)
        .put("JSESSIONID", sessionId)
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
