package org.jooby.ftl;

import org.jooby.Results;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue1095 extends ServerFeature {
  {
    use(new Ftl());

    get("/1095", req -> Results.html("org/jooby/ftl/index").put("model", "FreeMarker & XSS"));
  }

  @Test
  public void shouldEscapeHtmlData() throws Exception {
    request()
        .get("/1095")
        .expect("<html>org/jooby/ftl/index.html:org/jooby/ftl/index<body>FreeMarker &amp; XSS</body></html>");
  }
}
