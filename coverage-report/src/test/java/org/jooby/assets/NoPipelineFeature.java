package org.jooby.assets;

import org.jooby.Results;
import org.jooby.hbs.Hbs;
import org.junit.Test;

public class NoPipelineFeature extends AssetsBase {

  {

    use(assets("dev", "basedir", "org/jooby/assets", "fileset",
        map("home", list("js/index.js", "css/index.css")), "watch", false));

    use(new Hbs("/org/jooby/assets"));
    use(new Assets());

    get("/", () -> Results.html("index"));
  }

  @Test
  public void nopipeline() throws Exception {
    request()
      .get("/")
      .expect("[/org/jooby/assets/css/index.css]\n" +
          "<link href=\"/org/jooby/assets/css/index.css\" rel=\"stylesheet\">\n" +
          "\n" +
          "[/org/jooby/assets/js/index.js]\n" +
          "<script src=\"/org/jooby/assets/js/index.js\"></script>\n");
  }

  @Test
  public void indexjs() throws Exception {
    request()
      .get("/org/jooby/assets/js/index.js")
      .expect("function index() {\n" +
          "}\n" +
          "");
  }
}
