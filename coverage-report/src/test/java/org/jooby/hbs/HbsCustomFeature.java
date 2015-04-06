package org.jooby.hbs;

import org.jooby.Results;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.github.jknack.handlebars.io.ClassPathTemplateLoader;

public class HbsCustomFeature extends ServerFeature {

  {
    use(new Hbs().doWith((h, c) -> {
      h.with(new ClassPathTemplateLoader("/org/jooby/hbs", ".html"));
    }));

    get("/", req -> Results.html("index").put("model", req.param("model").value()));
  }

  @Test
  public void hbs() throws Exception {
    request()
        .get("/?model=jooby")
        .expect("<html><body>jooby</body></html>");
  }

}
