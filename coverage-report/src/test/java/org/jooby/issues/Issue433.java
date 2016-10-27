package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue433 extends ServerFeature {

  public static class Bean {
    String q;
  }

  {
    get("/433", req -> {
      Bean bean = req.params(Bean.class);
      return bean.q;
    });

    post("/433", req -> {
      Bean bean = req.form(Bean.class);
      return bean.q;
    });
  }

  @Test
  public void shortCutMethods() throws Exception {
    request()
        .get("/433?q=q1")
        .expect("q1");

    request()
        .post("/433")
        .form()
        .add("q", "q2")
        .expect("q2");
  }
}
