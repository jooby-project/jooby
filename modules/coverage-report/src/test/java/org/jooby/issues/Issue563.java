package org.jooby.issues;

import org.jooby.Results;
import org.jooby.test.ServerFeature;
import org.jooby.thymeleaf.Thl;
import org.junit.Test;

public class Issue563 extends ServerFeature {

  {
    use(new Thl());

    get("/", req -> Results.html("org/jooby/thl/index").put("model", req.param("model").value()));
  }

  @Test
  public void thl() throws Exception {
    request()
        .get("/?model=jooby")
        .expect(
            "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<body>\n" +
            "<p>\n" +
            "    Hello <span>jooby</span>!!!\n" +
            "</p>\n" +
            "</body>\n" +
            "</html>\n" +
            "");
  }
}
