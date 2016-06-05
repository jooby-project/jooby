package org.jooby.issues;

import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Issue393 extends ServerFeature {

  public static class Data {}

  @Path("/r")
  public static class Resource {
    @Path("/param")
    public String param(final String value) {
      return value;
    }

    @Path("/type")
    public Object type(final int value) {
      return value;
    }
  }

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  {
    get("/param", req -> {
      return req.param("value").value();
    });

    get("/no-parser", req -> {
      return req.param("value").to(Data.class);
    });

    post("/body", req -> {
      return req.body().to(Data.class);
    });

    get("/type", req -> {
      return req.param("value").intValue();
    });

    use(Resource.class);

    err((req, rsp, err) -> {
      log.error("issue 393 {}", req.route(), err);
      rsp.send(err.getMessage());
    });
  }

  @Test
  public void missingParam() throws Exception {
    request()
        .get("/param")
        .expect(400)
        .expect("Bad Request(400): Required parameter 'value' is not present");

    request()
        .get("/r/param")
        .expect(400)
        .expect("Bad Request(400): Required parameter 'value' is not present");
  }

  @Test
  public void typeMismatch() throws Exception {
    request()
        .get("/type?value=x")
        .expect(400)
        .expect("Bad Request(400): Failed to parse parameter 'value' to 'int'");

    request()
        .get("/r/type?value=x")
        .expect(400)
        .expect("Bad Request(400): Failed to parse parameter 'value' to 'int'");
  }

  @Test
  public void body() throws Exception {
    request()
        .post("/body")
        .expect(415)
        .expect("Unsupported Media Type(415): Failed to parse body to 'org.jooby.issues.Issue393$Data'");
  }
}
