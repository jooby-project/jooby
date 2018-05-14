package org.jooby.issues;

import java.util.Optional;

import org.jooby.Err;
import org.jooby.Status;
import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue23 extends ServerFeature {

  public static class Mvc {

    @GET
    @Path("/")
    public String handle(final Optional<String> value) {
      return value.orElseThrow(() -> new Err(Status.NOT_FOUND));
    }
  }

  {
    use(Mvc.class);
  }

  @Test
  public void shouldGetStatusWhenErrIsThrownFromMvcRoute() throws Exception {
    request()
      .get("/")
      .expect(404);
  }

}
