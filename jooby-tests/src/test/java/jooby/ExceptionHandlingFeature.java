package jooby;

import jooby.mvc.GET;
import jooby.mvc.Path;

import org.apache.http.client.fluent.Request;
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
        Multibinder<BodyConverter> converters = Multibinder.newSetBinder(binder,
            BodyConverter.class);
        converters.addBinding().toInstance(TestBodyConverter.HTML);
        converters.addBinding().toInstance(TestBodyConverter.JSON);
      }
    });

    get("/error", (req, resp) -> resp.send(null));

    route(Resource.class);
  }

  private static final String CHROME_ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8";

  @Test
  public void htmlError() throws Exception {
    assertHttp(Request.Get(uri("error").build()).addHeader("Accept", CHROME_ACCEPT))
        .status(Response.Status.SERVER_ERROR)
        .type(MediaType.valueOf("text/html; charset=UTF-8"))
        .done();
    ;
  }

  @Test
  public void jsonError() throws Exception {
    assertHttp(Request.Get(uri("error").build()).addHeader("Accept", "application/json"))
        .status(Response.Status.SERVER_ERROR)
        .type(MediaType.valueOf("application/json; charset=UTF-8"))
        .done();
  }

}
