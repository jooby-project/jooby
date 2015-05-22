package org.jooby;

import org.jooby.ftl.Ftl;
import org.jooby.hbs.Hbs;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class TwoViewEngineFeature extends ServerFeature {

  {

    use(new Ftl("/org/jooby/views", ".ftl"));
    use(new Hbs("/org/jooby/views", ".hbs"));

    get("/:view", (req, rsp) -> {
      String view = req.param("view").value();
      rsp.send(Results.html(view).put("view", view));
    });

  }

  @Test
  public void hbs() throws Exception {
    request()
        .get("/hbs")
        .expect("hbs");
  }

  @Test
  public void freemarker() throws Exception {
    request()
        .get("/ftl")
        .expect("ftl");
  }

  @Test
  public void notFound() throws Exception {
    request()
        .get("/notfound")
        .expect(404);
  }

}
