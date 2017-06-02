package org.jooby.jade;

import org.jooby.Results;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class JadeFeature extends ServerFeature {

  {
    use(new Jade(".html"));

    get("/", req -> Results.html("org/jooby/jade/index").put("model", req.param("model").value()));
  }

  @Test
  public void jade() throws Exception {
    request()
        .get("/?model=jooby")
        .expect("<!DOCTYPE html>\n" +
            "<html>\n" +
            "  <head>\n" +
            "    <title>org/jooby/jade/index</title>\n" +
            "  </head>\n" +
            "  <body>\n" +
            "    <p>jooby</p>\n" +
            "  </body>\n" +
            "</html>");
  }

}
