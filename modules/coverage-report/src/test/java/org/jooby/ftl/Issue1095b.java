package org.jooby.ftl;

import freemarker.core.UndefinedOutputFormat;
import org.jooby.Results;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue1095b extends ServerFeature {
  {
    use(new Ftl().doWith(freemarker -> {
      freemarker.setOutputFormat(UndefinedOutputFormat.INSTANCE);
    }));

    get("/1095", req -> Results.html("org/jooby/ftl/index").put("model", "FreeMarker & XSS"));
  }

  @Test
  public void shouldNotEscapeHtmlDataWhenOutputFormatIsSet() throws Exception {
    request()
        .get("/1095")
        .expect("<html>org/jooby/ftl/index.html:org/jooby/ftl/index<body>FreeMarker & XSS</body></html>");
  }
}
