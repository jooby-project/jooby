package org.jooby.issues;

import org.jooby.mvc.GET;
import org.jooby.mvc.Local;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class Issue1207 extends ServerFeature {

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  public @interface Role {
    String[] value() default "";
  }

  @Path("/1207")
  public static class Controller1207 {

    @GET
    @Role({})
    public String service() {
      return "1207";
    }
  }

  {
    use(Controller1207.class);
  }

  @Test
  public void shouldNotFailOnEmptyAttribute() throws Exception {
    request()
        .get("/1207")
        .expect("1207");
  }
}
