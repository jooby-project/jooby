package jooby;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import jooby.mvc.GET;
import jooby.mvc.Path;
import jooby.mvc.Template;

import org.apache.http.client.fluent.Request;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;
import com.typesafe.config.Config;

public class TemplateEngineFeature extends ServerFeature {

  @Path("/r")
  public static class Resource {

    @Path("/view")
    @GET
    public Viewable view() throws IOException {
      return Viewable.of("test", "model");
    }

    @Path("/view/template")
    @Template("template")
    @GET
    public Object template() throws IOException {
      return "model";
    }
  }

  {
    use(new JoobyModule() {
      @Override
      public void configure(final Mode mode, final Config config, final Binder binder)
          throws Exception {
        Multibinder<BodyConverter> converters = Multibinder.newSetBinder(binder,
            BodyConverter.class);
        converters.addBinding().toInstance(TestBodyConverter.HTML);
      }
    });

    get("/view", (req, resp) -> {
      resp.send(Viewable.of("test", "model"));
    });

    route(Resource.class);
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
