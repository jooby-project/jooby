/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.sitemap;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.jooby.Route;
import org.jooby.internal.sitemap.JSitemap;

import cz.jiripinkas.jsitemapgenerator.WebPage;
import cz.jiripinkas.jsitemapgenerator.generator.SitemapGenerator;
import javaslang.Function1;

/**
 * <h1>sitemap</h1>
 * <p>
 * Generate <a href="https://en.wikipedia.org/wiki/Sitemaps">sitemap.xml</a> files using
 * <a href="https://github.com/jirkapinkas/jsitemapgenerator">jsitemapgenerator</a>.
 * </p>
 *
 * <h2>usage</h2>
 * <pre>{@code
 * {
 *   use(new Sitemap());
 *
 *   get("/page1", ..);
 *   get("/page2", ..);
 * }
 * }</pre>
 *
 * <p>
 * The module exports a <code>/sitemap.xml</code> route.
 * </p>
 *
 * <h2>baseurl</h2>
 * <p>
 * The <code>sitemap.xml</code> specification requires an absolute url. The way we provide this
 * absolute url is at creation time or using the <code>sitemap.url</code> property:
 * </p>
 * <pre>{@code
 * {
 *   use(new Sitemap("https://foo.bar"));
 *
 *   get("/page1", ..);
 *   get("/page2", ..);
 * }
 * }</pre>
 *
 * or
 *
 * <pre>
 * sitemap.url = "http://foo.bar"
 * </pre>
 *
 * <h2>customize</h2>
 * <p>
 * The sitemap generator builds a <code>sitemap.xml</code> file with <code>loc</code> elements. You
 * can customize the output in one of two ways:
 * </p>
 *
 * <h3>declarative</h3>
 *
 * <pre>
 * {
 *
 *   get("/")
 *     .get("/page1", ..)
 *     .get("/page2", ..)
 *     .attr("changefreq", "weekly")
 *     .attr("priority", "1");
 * }
 * </pre>
 *
 * <p>
 * We first group route under a common path: <code>/</code> and add some routers. Then for each
 * router we set the <code>changefrequency</code> and <code>priority</code>.
 * </p>
 *
 * <h3>programmatically</h3>
 *
 * <pre>{@code
 * {
 *
 *   use(new Sitemap().with(r -> {
 *     WebPage page = new WebPage();
 *     page.setName(r.pattern());
 *     page.setChangeFreq(ChangeFreq.ALWAYS);
 *     page.setPriority(1);
 *     return Arrays.asList(page);
 *   }));
 *
 *   get("/")
 *     .get("/page1", ..)
 *     .get("/page2", ..);
 * }
 * }</pre>
 *
 * <p>
 * Here we built {@link WebPage} objects and set frequency and priority.
 * </p>
 *
 * <h2>dynamic page generation</h2>
 * <p>
 * Suppose you have a <strong>product</strong> route dynamically mapped as:
 * </p>
 *
 * <pre>{@code
 * {
 *   get("/products/:sku", ...);
 * }
 * }</pre>
 *
 * <h3>
 * How do you generate urls for all your products?
 * </h3>
 *
 * <p>
 * Dynamic urls are supported via custom {@link WebPageProvider}:
 * </p>
 *
 * <pre>{@code
 * {
 *   use(new Sitemap().with(SKUPageProvider.class));
 *
 *   get("/products/:sku", ...);
 * }
 * }</pre>
 *
 * SKUPageProvider.java:
 * <pre>{@code
 * public class SKUPageProvider implements WebPageProvider {
 *
 *   private MyDatabase db;
 *
 *   &#64;Inject
 *   public SKUPageProvider(MyDatabase db) {
 *     this.db = db;
 *   }
 *
 *   public List<WebPage> apply(Route.Definition route) {
 *     if (route.pattern().startsWith("/products")) {
 *       // multiple urls
 *       return db.findSKUS().stream().map(sku -> {
 *           WebPage webpage = new WebPage();
 *           webpage.setName(route.reverse(sku));
 *           return webpage;
 *         }).collect(Collectors.toList());
 *     }
 *     // single url
 *     WebPage webpage = new WebPage();
 *     webpage.setName(route.pattern());
 *     return Arrays.asList(webpage);
 *   }
 * }
 * }</pre>
 *
 * <h2>filter</h2>
 * <p>
 * The {@link #filter(java.util.function.Predicate)} option allows to excludes routes from final
 * output:
 * </p>
 *
 * <pre>{@code
 * {
 *   use(new Sitemap().filter(route -> !route.pattern().startsWith("/api")));
 * }
 * }</pre>
 *
 * @author edgar
 * @since 1.0.0.CR
 */
public class Sitemap extends JSitemap<Sitemap> {

  /**
   * Creates a new {@link Sitemap}.
   */
  public Sitemap() {
    this(Optional.empty());
  }

  /**
   * Creates a new {@link Sitemap} and set the base url.
   *
   * @param baseurl Base url to use.
   */
  public Sitemap(final String baseurl) {
    this(Optional.of(baseurl));
  }

  private Sitemap(final Optional<String> baseurl) {
    super(SITEMAP, baseurl, WebPageProvider.SITEMAP);
  }

  /**
   * <h2>filter</h2>
   * <p>
   * The {@link #filter(java.util.function.Predicate)} option allows to excludes routes from final
   * output:
   * </p>
   *
   * <pre>{@code
   * {
   *   use(new Sitemap().filter(route -> !route.pattern().startsWith("/api")));
   * }
   * }</pre>
   *
   */
  @Override
  public Sitemap filter(final Predicate<Route.Definition> filter) {
    return super.filter(filter);
  }

  /**
   * Set a custom {@link WebPageProvider}.
   *
   * <pre>{@code
   * {
   *
   *   use(new Sitemap().with(r -> {
   *     WebPage page = new WebPage();
   *     page.setName(r.pattern());
   *     page.setChangeFreq(ChangeFreq.ALWAYS);
   *     page.setPriority(1);
   *     return Arrays.asList(page);
   *   }));
   * }
   * }</pre>
   *
   * @param wpp A web page provider.
   */
  @Override
  public Sitemap with(final WebPageProvider wpp) {
    return super.with(wpp);
  }

  /**
   * Set a custom {@link WebPageProvider}.
   *
   * <pre>{@code
   * {
   *   use(new Sitemap().with(MyWebPageProvider.class));
   * }
   * }</pre>
   *
   * The <code>MyWebPageProvider</code> will be created and injected by Guice.
   *
   * @param wpp A web page provider.
   */
  @Override
  public Sitemap with(final Class<? extends WebPageProvider> wpp) {
    return super.with(wpp);
  }

  @Override
  protected Function1<List<WebPage>, String> gen(final String baseurl) {
    return pages -> {
      SitemapGenerator generator = new SitemapGenerator(baseurl);
      generator.addPages(pages);
      return generator.constructSitemapString();
    };
  }

}
