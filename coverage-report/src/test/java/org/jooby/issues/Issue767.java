package org.jooby.issues;

import org.jooby.Jooby;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue767 extends ServerFeature {

  public static class Service {
    public String doWork() {
      return "OK";
    }
  }

  public static class Foo extends Jooby {
    {
      get("/foo", () -> require(Service.class).doWork());
    }
  }

  {
    get("/767", () -> require(Service.class).doWork());

    use("/767", new Foo());
  }

  @Test
  public void shouldHaveAccessFromTopApp() throws Exception {
    request()
        .get("/767")
        .expect("OK");
  }

  @Test
  public void shouldHaveAccessFromSubApp() throws Exception {
    request()
        .get("/767/foo")
        .expect("OK");
  }

}
