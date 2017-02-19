package org.jooby.issues;

import cz.jiripinkas.jsitemapgenerator.WebPage;
import org.jooby.sitemap.Sitemap;
import org.jooby.sitemap.WebPageProvider;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import java.util.Arrays;
import java.util.stream.Collectors;

public class Issue342 extends ServerFeature {

  {
    use(new Sitemap().with(r -> {
      if (r.pattern().startsWith("/tags")) {
        // expand tags
        return Arrays.asList("foo", "bar").stream()
            .map(tag -> {
              WebPage page = WebPageProvider.SITEMAP.apply(r).get(0);
              page.setName(r.reverse(tag));
              return page;
            }).collect(Collectors.toList());
      }
      return WebPageProvider.SITEMAP.apply(r);
    }));

    get("/", () -> "x");

    get("/tags", () -> "x");

    get("/tags/:name", () -> "x");

    post("/ignored", () -> "x");
  }

  @Test
  public void sitemap() throws Exception {
    request().get("/sitemap.xml")
        .expect("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n" +
            "<url>\n" +
            "<loc>http://localhost:" + port + "/</loc>\n" +
            "</url>\n" +
            "<url>\n" +
            "<loc>http://localhost:" + port + "/tags</loc>\n" +
            "</url>\n" +
            "<url>\n" +
            "<loc>http://localhost:" + port + "/tags/bar</loc>\n" +
            "</url>\n" +
            "<url>\n" +
            "<loc>http://localhost:" + port + "/tags/foo</loc>\n" +
            "</url>\n" +
            "</urlset>")
        .header("Content-Type", "application/xml;charset=utf-8");
  }

}
