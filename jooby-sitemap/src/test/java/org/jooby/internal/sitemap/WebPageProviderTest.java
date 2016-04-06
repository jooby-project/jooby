package org.jooby.internal.sitemap;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.jooby.Route;
import org.jooby.sitemap.WebPageProvider;
import org.junit.Test;

import cz.jiripinkas.jsitemapgenerator.ChangeFreq;
import cz.jiripinkas.jsitemapgenerator.WebPage;

public class WebPageProviderTest {

  @Test
  public void defsitemap() {
    WebPageProvider sitemap = WebPageProvider.SITEMAP;
    List<WebPage> pages = sitemap.apply(new Route.Definition("get", "/path", () -> ""));
    assertEquals(1, pages.size());
    assertEquals("/path", pages.get(0).getName());
    assertEquals(null, pages.get(0).getChangeFreq());
    assertEquals(null, pages.get(0).getPriority());
  }

  @Test
  public void sitemapWithFreq() {
    WebPageProvider sitemap = WebPageProvider.SITEMAP;
    List<WebPage> pages = sitemap
        .apply(new Route.Definition("get", "/path", () -> "").attr("changefreq", "always"));
    assertEquals(1, pages.size());
    assertEquals("/path", pages.get(0).getName());
    assertEquals(ChangeFreq.ALWAYS, pages.get(0).getChangeFreq());
    assertEquals(null, pages.get(0).getPriority());
  }

  @Test
  public void sitemapWithPriority() {
    WebPageProvider sitemap = WebPageProvider.SITEMAP;
    List<WebPage> pages = sitemap
        .apply(new Route.Definition("get", "/path", () -> "")
            .attr("changefreq", "always")
            .attr("priority", "1.0"));
    assertEquals(1, pages.size());
    assertEquals("/path", pages.get(0).getName());
    assertEquals(ChangeFreq.ALWAYS, pages.get(0).getChangeFreq());
    assertEquals(0, 1.0d, pages.get(0).getPriority());
  }

}
