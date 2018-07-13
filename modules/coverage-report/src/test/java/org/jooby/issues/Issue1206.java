package org.jooby.issues;

import org.jooby.mvc.GET;
import org.jooby.mvc.Local;
import org.jooby.mvc.Path;
import org.jooby.test.JoobySuite;
import org.jooby.test.ServerFeature;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class Issue1206 extends ServerFeature {

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  public @interface Role {
    String value() default "";
  }

  @Path("/1206")
  public static class Controller1206 {

    @GET
    @Role("foo")
    public String service(@Local String bar) {
      return bar;
    }
  }

  {
    use("*", (req, rsp) -> {
      req.set("bar", req.route().attr("role")); // <- null
    });

    use(Controller1206.class);
  }

  @Test
  public void shouldAccessToSingleAttributeFromRouteChain() throws Exception {
    request()
        .get("/1206")
        .expect("foo");
  }
}
