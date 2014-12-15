package org.jooby.hbs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.apache.http.client.fluent.Request;
import org.jooby.View;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;

// TODO: make me a unit test
public class HbsCustomFeature extends ServerFeature {

  {
    Handlebars handlebars = new Handlebars(new ClassPathTemplateLoader("/", ".html"));
    use(new Hbs(handlebars).doWith(h -> assertSame(handlebars, h)));

    get("/", req -> View.of("index", req.param("model").stringValue()));
  }

  @Test
  public void hbs() throws Exception {
    assertEquals("<html><body>jooby</body></html>",
        Request.Get(uri("/").addParameter("model", "jooby").build()).execute()
            .returnContent().asString());
  }

}
