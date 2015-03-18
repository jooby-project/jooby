package org.jooby.hbs;

import static org.junit.Assert.assertSame;

import org.jooby.View;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;

public class HbsCustomFeature extends ServerFeature {

  {
    Handlebars handlebars = new Handlebars(new ClassPathTemplateLoader("/org/jooby/hbs", ".html"));
    use(new Hbs(handlebars).doWith(h -> assertSame(handlebars, h)));

    get("/", req -> View.of("index", "model", req.param("model").value()));
  }

  @Test
  public void hbs() throws Exception {
    request()
        .get("/?model=jooby")
        .expect("<html><body>jooby</body></html>");
  }

}
