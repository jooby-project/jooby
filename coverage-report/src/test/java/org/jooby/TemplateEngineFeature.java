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
      return Results.html("test").put("this", "model");
    }

  }

  {
    use((mode, config, binder) -> {
      Multibinder<BodyFormatter> converters = Multibinder.newSetBinder(binder,
          BodyFormatter.class);
      converters.addBinding().toInstance(BodyConverters.toHtml);
    });

    get("/view", (req, resp) -> {
      resp.send(Results.html("test").put("this", "model"));
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
