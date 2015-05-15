package org.jooby;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class ViewWithExplicitEngineFeature extends ServerFeature {

  {

    renderer(new View.Engine() {

      @Override
      public String name() {
        return "hbs";
      }

      @Override
      public void render(final View viewable, final Renderer.Context ctx) throws Exception {
        ctx.send(name());
      }
    });

    renderer(new View.Engine() {

      @Override
      public String name() {
        return "freemarker";
      }

      @Override
      public void render(final View viewable, final Renderer.Context ctx) throws Exception {
        ctx.send(name());
      }
    });

    get("/:engine", (req, rsp) -> {
      String engine = req.param("engine").value();
      rsp.send(Results.html("view").put("this", new Object()).engine(engine));
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
        .get("/freemarker")
        .expect("freemarker");
  }

}
