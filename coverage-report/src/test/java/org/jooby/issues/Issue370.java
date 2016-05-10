package org.jooby.issues;

import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue370 extends ServerFeature {

  @Path("/")
  public static class S {

    @Path("/s")
    public String doSomething() {
      return "GET";
    }

  }

  {
     use(S.class);
  }

  @Test
  public void mvcShouldDefaultToGet() throws Exception {
    request()
        .get("/s")
        .expect("GET");
  }

}
