package org.jooby.issues;

import org.jooby.Results;
import org.jooby.assets.Assets;
import org.jooby.assets.AssetsBase;
import org.jooby.hbs.Hbs;
import org.junit.Test;

public class Issue553 extends AssetsBase {

  {

    use(assets("dev",
        "basedir", "org/jooby/assets",
        "dev", hash("prefix", "https://foo.cdn.org"),
        "fileset", hash("home", list("js/index.js", "css/index.css")), "watch", false));

    use(new Hbs("/org/jooby/assets"));
    use(new Assets());

    get("/553", () -> Results.html("index"));
  }

  @Test
  public void nopipeline() throws Exception {
    request()
        .get("/553")
        .expect("[/org/jooby/assets/css/index.css]\n" +
            "<link href=\"https://foo.cdn.org/org/jooby/assets/css/index.css\" rel=\"stylesheet\">\n" +
            "\n" +
            "[/org/jooby/assets/js/index.js]\n" +
            "<script src=\"https://foo.cdn.org/org/jooby/assets/js/index.js\"></script>\n");
  }

}
