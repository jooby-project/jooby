package org.jooby;

import org.jooby.Body;
import org.jooby.Env;
import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;
import com.typesafe.config.Config;

public class ExceptionHandlingFeature extends ServerFeature {

  @Path("/r")
  public static class Resource {

    @Path("/error")
    @GET
    public String error() {
      return null;
    }

  }

  {

    use((final Env mode, final Config config, final Binder binder) -> {
      Multibinder<Body.Formatter> converters = Multibinder.newSetBinder(binder,
          Body.Formatter.class);
      converters.addBinding().toInstance(BodyConverters.toHtml);
      converters.addBinding().toInstance(BodyConverters.toJson);
    });

    get("/error", (req, rsp) -> rsp.send(null));

    use(Resource.class);
  }

  private static final String CHROME_ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8";

  @Test
  public void htmlError() throws Exception {
    request()
        .get("/error")
        .header("Accept", CHROME_ACCEPT)
        .expect(500)
        .header("Content-Type", "text/html;charset=UTF-8");
  }

  @Test
  public void jsonError() throws Exception {
    request()
        .get("/error")
        .header("Accept", "application/json")
        .expect(500)
        .header("Content-Type", "application/json;charset=UTF-8");
  }

}
