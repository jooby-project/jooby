package org.jooby;

import java.io.IOException;

import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.google.inject.multibindings.Multibinder;

public class TemplateEngineFeature extends ServerFeature {

  @Path("/r")
  public static class Resource {

    @Path("/view")
    @GET
    public View view() throws IOException {
      return View.of("test", "this", "model");
    }

  }

  {
    use((mode, config, binder) -> {
      Multibinder<Body.Formatter> converters = Multibinder.newSetBinder(binder,
          Body.Formatter.class);
      converters.addBinding().toInstance(BodyConverters.toHtml);
    });

    get("/view", (req, resp) -> {
      resp.send(View.of("test", "this", "model"));
    });

    use(Resource.class);
  }

  @Test
  public void view() throws Exception {
    request()
        .get("/view")
        .expect("<html><body>test: {this=model}</body></html>");

    request()
        .get("/r/view")
        .expect("<html><body>test: {this=model}</body></html>");
  }

}
