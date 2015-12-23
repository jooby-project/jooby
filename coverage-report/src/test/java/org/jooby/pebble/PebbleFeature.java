package org.jooby.pebble;

import org.jooby.Results;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class PebbleFeature extends ServerFeature {

  {
    use(new Pebble());

    get("/", req -> Results.html("org/jooby/pebble/index").put("model", req.param("model").value()));
  }

  @Test
  public void pebble() throws Exception {
    request()
        .get("/?model=jooby")
        .expect("<html><title>org/jooby/pebble/index</title><body>jooby</body></html>");
  }

}
