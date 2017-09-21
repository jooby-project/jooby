package org.jooby.internal.sitemap;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import cz.jiripinkas.jsitemapgenerator.WebPage;
import cz.jiripinkas.jsitemapgenerator.generator.SitemapGenerator;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import org.jooby.Env;
import org.jooby.Router;
import org.jooby.sitemap.Sitemap;
import org.jooby.sitemap.WebPageProvider;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.jooby.funzy.Throwing;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Sitemap.class, SitemapHandler.class, SitemapGenerator.class})
public class SitemapTest {

  private Block confWithSiteMapUrl = unit -> {
    Config config = unit.get(Config.class);
    expect(config.hasPath("sitemap.url")).andReturn(true);
    expect(config.getString("sitemap.url")).andReturn("http://foo.org");
  };

  @SuppressWarnings("unchecked")
  private Block defwpp = unit -> {
    LinkedBindingBuilder<WebPageProvider> lbb = unit.mock(LinkedBindingBuilder.class);
    lbb.toInstance(WebPageProvider.SITEMAP);

    Binder binder = unit.get(Binder.class);
    expect(binder.bind(Key.get(WebPageProvider.class, Names.named("/sitemap.xml")))).andReturn(lbb);
  };

  private Block route = unit -> {

    Router routes = unit.mock(Router.class);
    expect(routes.get("/sitemap.xml", unit.get(SitemapHandler.class))).andReturn(null);

    Env env = unit.get(Env.class);
    expect(env.router()).andReturn(routes);
  };

  private Block handler = unit -> {
    SitemapHandler handler = unit.constructor(SitemapHandler.class)
        .args(String.class, Predicate.class, Throwing.Function.class)
        .build(eq("/sitemap.xml"), unit.capture(Predicate.class),
            unit.capture(Throwing.Function.class));

    unit.registerMock(SitemapHandler.class, handler);
  };

  @Test
  public void configure() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(defwpp)
        .expect(handler)
        .expect(route)
        .run(unit -> {
          new Sitemap("http://localhost")
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void configureWithoutBaseUrl() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.hasPath("sitemap.url")).andReturn(false);
          expect(config.getConfig("application")).andReturn(config);
          expect(config.getString("host")).andReturn("localhost");
          expect(config.getString("port")).andReturn("8080");
          expect(config.getString("path")).andReturn("/");
        })
        .expect(defwpp)
        .expect(handler)
        .expect(route)
        .run(unit -> {
          new Sitemap()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void withWPPInstance() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, WebPageProvider.class)
        .expect(confWithSiteMapUrl)
        .expect(unit -> {
          LinkedBindingBuilder<WebPageProvider> lbb = unit.mock(LinkedBindingBuilder.class);
          lbb.toInstance(unit.get(WebPageProvider.class));

          Binder binder = unit.get(Binder.class);
          expect(binder.bind(Key.get(WebPageProvider.class, Names.named("/sitemap.xml"))))
              .andReturn(lbb);
        })
        .expect(handler)
        .expect(route)
        .run(unit -> {
          new Sitemap()
              .with(unit.get(WebPageProvider.class))
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void withWPPType() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(confWithSiteMapUrl)
        .expect(unit -> {
          LinkedBindingBuilder<WebPageProvider> lbb = unit.mock(LinkedBindingBuilder.class);
          expect(lbb.to(WebPageProvider.class)).andReturn(null);

          Binder binder = unit.get(Binder.class);
          expect(binder.bind(Key.get(WebPageProvider.class, Names.named("/sitemap.xml"))))
              .andReturn(lbb);
        })
        .expect(handler)
        .expect(route)
        .run(unit -> {
          new Sitemap()
              .with(WebPageProvider.class)
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Test
  public void build() throws Exception {
    List<WebPage> pages = Collections.emptyList();
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(confWithSiteMapUrl)
        .expect(defwpp)
        .expect(handler)
        .expect(route)
        .expect(unit -> {
          SitemapGenerator gen = unit.constructor(SitemapGenerator.class)
              .args(String.class)
              .build("http://foo.org");
          gen.addPages(pages);
          expect(gen.constructSitemapString()).andReturn("<xml>");
        })
        .run(unit -> {
          new Sitemap()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, unit -> {
          Throwing.Function fn = unit.captured(Throwing.Function.class).iterator().next();
          fn.apply(pages);
        });
  }

}
