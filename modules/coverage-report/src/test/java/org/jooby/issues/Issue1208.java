package org.jooby.issues;

import org.jooby.Route;
import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;

public class Issue1208 extends ServerFeature {

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  public @interface Role {
    String[] value() default "";
  }

  @Path("/1208")
  public static class Controller1208 {

    @GET
    @Role("first")
    public String first(Route route) {
      return Arrays.toString((String[]) route.attr("role"));
    }

    @GET
    @Role("second")
    public String second(Route route) {
      return Arrays.toString((String[]) route.attr("role"));
    }
  }

  {
    use(Controller1208.class);
  }

  @Test
  public void shouldNotFailOnEmptyAttribute() throws Exception {
    request()
        .get("/1208")
        .expect("[first]");
  }
}
