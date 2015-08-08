package org.jooby.sass;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class SassFeature extends ServerFeature {

  {
    use(new Sass("/css/**", "/org/jooby/sass/{0}"));
  }

  @Test
  public void handler() throws Exception {
    request()
        .get("/css/sass.css")
        .expect("body {\n" +
            "\tfont: 100% Helvetica, sans-serif;\n" +
            "\tcolor: #333;\n" +
            "}");
  }

}
