package org.jooby.hbs;

import org.jooby.View;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class HbsFeature extends ServerFeature {

  {
    use(new Hbs());

    get("/", req -> View.of("/org/jooby/hbs/index", req.param("model").value()));
  }

  @Test
  public void hbs() throws Exception {
    request()
        .get("/?model=jooby")
        .expect("<html><body>jooby</body></html>");
  }

}
