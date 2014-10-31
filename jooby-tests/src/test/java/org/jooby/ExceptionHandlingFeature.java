package org.jooby;

import org.apache.http.client.fluent.Request;
import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
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

    use(new Jooby.Module() {

      @Override
      public void configure(final Mode mode, final Config config, final Binder binder)
          throws Exception {
        Multibinder<Body.Formatter> converters = Multibinder.newSetBinder(binder,
            Body.Formatter.class);
        converters.addBinding().toInstance(BodyConverters.toHtml);
        converters.addBinding().toInstance(BodyConverters.toJson);
      }
    });

    get("/error", (req, resp) -> resp.send(null));

    use(Resource.class);
  }

  private static final String CHROME_ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8";

  @Test
  public void htmlError() throws Exception {
    assertHttp(Request.Get(uri("error").build()).addHeader("Accept", CHROME_ACCEPT))
        .status(Status.SERVER_ERROR)
        .type(MediaType.valueOf("text/html; charset=utf-8"))
        .done();
    ;
  }

  @Test
  public void jsonError() throws Exception {
    assertHttp(Request.Get(uri("error").build()).addHeader("Accept", "application/json"))
        .status(Status.SERVER_ERROR)
        .type(MediaType.valueOf("application/json; charset=utf-8"))
        .done();
  }

}
