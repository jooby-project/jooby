package jooby;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;

public class Command implements Route {

  private String name;

  @Inject
  public Command(@Named("name") final String name, final EntityManager em) {
    this.name = name;
  }

  @Override
  public void handle(final Request request, final Response response) throws Exception {
    response.send("Hello " + name);
  }

}
