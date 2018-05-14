package org.jooby.ftl;

import org.jooby.Results;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class FtlFeature extends ServerFeature {

  {
    use(new Ftl());

    get("/", req -> Results.html("org/jooby/ftl/index").put("model", req.param("model").value()));
  }

  @Test
  public void freemarker() throws Exception {
    request()
        .get("/?model=jooby")
        .expect("<html>org/jooby/ftl/index.html:org/jooby/ftl/index<body>jooby</body></html>");
  }

}
