package org.jooby.issues;

import org.jooby.sitemap.Sitemap;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue342b extends ServerFeature {

  {
    use(new Sitemap().filter(r -> false));

    get("/", () -> "x");

    get("/tags/:name", () -> "x");

    post("/ignored", () -> "x");
  }

  @Test
  public void sitemap() throws Exception {
    request().get("/sitemap.xml")
        .expect("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n" +
            "</urlset>")
        .header("Content-Type", "application/xml;charset=utf-8");
  }

}
