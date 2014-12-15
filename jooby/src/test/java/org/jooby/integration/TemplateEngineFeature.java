package org.jooby.integration;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.http.client.fluent.Request;
import org.jooby.Body;
import org.jooby.View;
import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.jooby.mvc.Viewable;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.google.inject.multibindings.Multibinder;

public class TemplateEngineFeature extends ServerFeature {

  @Path("/r")
  public static class Resource {

    @Path("/view")
    @GET
    public View view() throws IOException {
      return View.of("test", "model");
    }

    @Path("/view/template")
    @Viewable("template")
    @GET
    public Object template() throws IOException {
      return "model";
    }
  }

  {
    use((mode, config, binder) -> {
      Multibinder<Body.Formatter> converters = Multibinder.newSetBinder(binder,
          Body.Formatter.class);
      converters.addBinding().toInstance(BodyConverters.toHtml);
    });

    get("/view", (req, resp) -> {
      resp.send(View.of("test", "model"));
    });

    use(Resource.class);
  }

  @Test
  public void view() throws Exception {
    assertEquals("<html><body>test: model</body></html>", Request.Get(uri("view").build())
        .execute().returnContent().asString());

    assertEquals("<html><body>test: model</body></html>", Request.Get(uri("r", "view").build())
        .execute().returnContent().asString());
  }

  @Test
  public void templateAnnotation() throws Exception {
    assertEquals("<html><body>template: model</body></html>",
        Request.Get(uri("r", "view", "template").build()).execute().returnContent().asString());
  }

}
