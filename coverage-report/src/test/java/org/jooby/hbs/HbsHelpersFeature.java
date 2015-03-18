package org.jooby.hbs;

import org.jooby.View;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class HbsHelpersFeature extends ServerFeature {

  public static class Helpers {

    public String itWorks() {
      return "oh yea";
    }
  }

  {
    use(new Hbs(Helpers.class));

    get("/", req -> View.of("org/jooby/hbs/helpers"));
  }

  @Test
  public void shouldInjectHelpers() throws Exception {
    request()
        .get("/")
        .expect("oh yea!");
  }

}
