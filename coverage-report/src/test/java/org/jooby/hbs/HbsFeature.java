package org.jooby.hbs;

import org.jooby.Results;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class HbsFeature extends ServerFeature {

  {
    use(new Hbs());

    get("/", req -> Results.html("org/jooby/hbs/index").put("model", req.param("model").value()));
  }

  @Test
  public void hbs() throws Exception {
    request()
        .get("/?model=jooby")
        .expect("<html><body>jooby</body></html>");
  }

}
