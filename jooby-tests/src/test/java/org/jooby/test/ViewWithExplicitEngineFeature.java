package org.jooby.test;

import static org.junit.Assert.assertEquals;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.jooby.View;
import org.jooby.Body.Writer;
import org.jooby.test.FilterFeature.HttpResponseValidator;
import org.junit.Test;

public class ViewWithExplicitEngineFeature extends ServerFeature {

  {

    use(new View.Engine() {

      @Override
      public String name() {
        return "hbs";
      }

      @Override
      public void render(final View viewable, final Writer writer) throws Exception {
        writer.text(w -> w.write(name()));
      }
    });

    use(new View.Engine() {

      @Override
      public String name() {
        return "freemarker";
      }
      @Override
      public void render(final View viewable, final Writer writer) throws Exception {
        writer.text(w -> w.write(name()));
      }
    });

    get("/:engine", (req, rsp) -> {
      String engine = req.param("engine").stringValue();
      rsp.send(View.of("view", new Object()).engine(engine));
    });

  }

  @Test
  public void engine() throws Exception {
    assertEquals("hbs", execute(GET(uri("/hbs")), (response) -> {
      assertEquals(200, response.getStatusLine().getStatusCode());
    }));

    assertEquals("freemarker", execute(GET(uri("/freemarker")), (response) -> {
      assertEquals(200, response.getStatusLine().getStatusCode());
    }));
  }

  private static Request GET(final URIBuilder uri) throws Exception {
    return Request.Get(uri.build());
  }

  private static Object execute(final Request request, final HttpResponseValidator validator)
      throws Exception {
    HttpResponse resp = request.execute().returnResponse();
    validator.validate(resp);
    return EntityUtils.toString(resp.getEntity());
  }
}
