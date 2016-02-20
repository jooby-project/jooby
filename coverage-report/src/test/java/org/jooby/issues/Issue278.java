package org.jooby.issues;

import org.jooby.mvc.GET;
import org.jooby.mvc.Local;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue278 extends ServerFeature {

  @Path("/issue278")
  public static class Resource {

    @GET
    public Object local(@Local final int local) {
      return local;
    }
  }

  {
    use("*", (req, rsp) -> {
      req.set("local", 678);
    });

    use(Resource.class);
  }

  @Test
  public void shouldNotDisplayStacktrace() throws Exception {
    request().get("/issue278").expect("678");
  }

}
