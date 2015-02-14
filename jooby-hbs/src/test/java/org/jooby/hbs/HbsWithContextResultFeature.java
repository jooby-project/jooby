package org.jooby.hbs;

import org.jooby.View;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.github.jknack.handlebars.Context;

public class HbsWithContextResultFeature extends ServerFeature {

  {
    use(new Hbs());

    get("/", req -> View.of("org/jooby/hbs/index", Context.newContext(req.param("model").stringValue())));
  }

  @Test
  public void hbs() throws Exception {
    request()
      .get("/?model=jooby")
      .expect("<html><body>jooby</body></html>");
  }

}
