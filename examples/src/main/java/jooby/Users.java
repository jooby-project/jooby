package jooby;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import jooby.mvc.GET;
import jooby.mvc.Path;

@Path("/user")
public class Users {

  private EntityManager em;

  @Inject
  public Users(final EntityManager em) {
    this.em = em;
  }

  @Path("/:id")
  @GET
  public User newUser(final String id) {
    User user = new User();
    user.setId(id);
    user.setFirstName("Ramiro");
    return user;
  }

}
