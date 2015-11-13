package org.jooby;

import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class RegexParamFeature extends ServerFeature {

  @Path("/r")
  public static class Resource {

    @Path("/regex/{id:\\d+}")
    @GET
    public Object regex(final int id) {
      return id;
    }

  }

  {
    get("/regex/{id:\\d+}", (req, resp) -> {
      int id = req.param("id").intValue();
      resp.send(id);
    });

    use(Resource.class);
  }

  @Test
  public void regex() throws Exception {
    request()
        .get("/regex/678")
        .expect("678");

    request()
        .get("/r/regex/678")
        .expect("678");

  }

  @Test
  public void notFound() throws Exception {

    request()
        .get("/r/regex/678x")
        .expect(404);

  }

}
