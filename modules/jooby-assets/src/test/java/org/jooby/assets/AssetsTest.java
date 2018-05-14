package org.jooby.assets;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Binder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import org.jooby.Env;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.Route.Definition;
import org.jooby.Route.Filter;
import org.jooby.Router;
import org.jooby.handlers.AssetHandler;
import org.jooby.internal.assets.AssetHandlerWithCompiler;
import org.jooby.internal.assets.AssetVars;
import org.jooby.internal.assets.LiveCompiler;
import org.jooby.test.MockUnit;
import org.jooby.funzy.Throwing;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Assets.class, AssetCompiler.class, Multibinder.class, LiveCompiler.class})
public class AssetsTest {

  @Test
  public void configure() throws Exception {
    Config conf = ConfigFactory.empty()
        .withValue("application.path", ConfigValueFactory.fromAnyRef("/path"))
        .withValue("assets.watch", ConfigValueFactory.fromAnyRef(false))
        .withValue("assets.etag", ConfigValueFactory.fromAnyRef(true))
        .withValue("assets.lastModified", ConfigValueFactory.fromAnyRef(true))
        .withValue("assets.cdn", ConfigValueFactory.fromAnyRef(""))
        .withValue("assets.cache.maxAge", ConfigValueFactory.fromAnyRef(-1));
    new MockUnit(Env.class, Binder.class, Request.class, Response.class,
        Route.Chain.class).expect(unit -> {
      AssetCompiler compiler = unit.constructor(AssetCompiler.class)
          .args(ClassLoader.class, Config.class)
          .build(Assets.class.getClassLoader(), conf);
      expect(compiler.patterns()).andReturn(Sets.newHashSet("/assets/**"));
      unit.registerMock(AssetCompiler.class, compiler);
    }).expect(unit -> {
      Definition assetVars = unit.mock(Definition.class);
      expect(assetVars.name("/assets/vars")).andReturn(assetVars);
      Router routes = unit.mock(Router.class);
      expect(routes.use(eq("*"), eq("*"), unit.capture(AssetVars.class))).andReturn(assetVars);

      Definition assetHandlerWithCompiler = unit.mock(Definition.class);
      expect(routes.get(eq("/assets/**"), isA(AssetHandlerWithCompiler.class)))
          .andReturn(assetHandlerWithCompiler);

      Env env = unit.get(Env.class);
      expect(env.router()).andReturn(routes);
    }).expect(unit -> {
      Env env = unit.get(Env.class);

      expect(env.name()).andReturn("dev");
    }).expect(unit -> {

      AssetCompiler compiler = unit.get(AssetCompiler.class);
      expect(compiler.fileset()).andReturn(Sets.newHashSet("home"));
      expect(compiler.styles("home")).andReturn(Lists.newArrayList("/home.css"));
      expect(compiler.scripts("home")).andReturn(Lists.newArrayList("/home.js"));

      Request req = unit.get(Request.class);
      expect(req.set("home_css", Lists.newArrayList("/home.css"))).andReturn(req);
      expect(req.set("home_styles", "<link href=\"/path/home.css\" rel=\"stylesheet\">\n"))
          .andReturn(req);

      expect(req.set("home_js", Lists.newArrayList("/home.js"))).andReturn(req);
      expect(req.set("home_scripts", "<script src=\"/path/home.js\"></script>\n"))
          .andReturn(req);

      unit.get(Route.Chain.class).next(req, unit.get(Response.class));
    }).run(unit -> {
      new Assets()
          .configure(unit.get(Env.class), conf, unit.get(Binder.class));
    }, unit -> {
      unit.captured(AssetVars.class).iterator().next().handle(unit.get(Request.class),
          unit.get(Response.class), unit.get(Route.Chain.class));
    });
  }

  @Test
  public void configureWithWatch() throws Exception {
    Config conf = ConfigFactory.empty()
        .withValue("application.path", ConfigValueFactory.fromAnyRef("/"))
        .withValue("assets.watch", ConfigValueFactory.fromAnyRef(true))
        .withValue("assets.etag", ConfigValueFactory.fromAnyRef(true))
        .withValue("assets.lastModified", ConfigValueFactory.fromAnyRef(true))
        .withValue("assets.cdn", ConfigValueFactory.fromAnyRef(""))
        .withValue("assets.cache.maxAge", ConfigValueFactory.fromAnyRef(-1));
    new MockUnit(Env.class, Binder.class, Request.class, Response.class,
        Route.Chain.class).expect(unit -> {
      AssetCompiler compiler = unit.constructor(AssetCompiler.class)
          .args(ClassLoader.class, Config.class)
          .build(Assets.class.getClassLoader(), conf);
      expect(compiler.patterns()).andReturn(Sets.newHashSet("/assets/**"));
      unit.registerMock(AssetCompiler.class, compiler);
    }).expect(unit -> {
      AssetCompiler compiler = unit.get(AssetCompiler.class);

      Definition assetVars = unit.mock(Definition.class);
      expect(assetVars.name("/assets/vars")).andReturn(assetVars);
      Router routes = unit.mock(Router.class);
      expect(routes.use(eq("*"), eq("*"), unit.capture(AssetVars.class))).andReturn(assetVars);

      Definition assetHandlerWithCompiler = unit.mock(Definition.class);
      expect(routes.get(eq("/assets/**"), isA(AssetHandlerWithCompiler.class)))
          .andReturn(assetHandlerWithCompiler);

      Definition liveCompilerRoute = unit.mock(Definition.class);
      expect(liveCompilerRoute.name("/assets/compiler")).andReturn(liveCompilerRoute);

      LiveCompiler liveCompiler = unit.constructor(LiveCompiler.class)
          .args(Config.class, AssetCompiler.class)
          .build(conf, compiler);
      expect(routes.use(eq("*"), eq("*"), eq(liveCompiler))).andReturn(liveCompilerRoute);

      Env env = unit.get(Env.class);
      expect(env.router()).andReturn(routes);
    }).expect(unit -> {
      Env env = unit.get(Env.class);

      expect(env.name()).andReturn("dev");
    }).expect(unit -> {
      Env env = unit.get(Env.class);
      expect(env.onStart(isA(Throwing.Runnable.class))).andReturn(env);
      expect(env.onStop(isA(Throwing.Runnable.class))).andReturn(env);
    }).expect(unit -> {
      AssetCompiler compiler = unit.get(AssetCompiler.class);
      expect(compiler.fileset()).andReturn(Sets.newHashSet("home"));
      expect(compiler.styles("home")).andReturn(Lists.newArrayList("/home.css"));
      expect(compiler.scripts("home")).andReturn(Lists.newArrayList("/home.js"));

      Request req = unit.get(Request.class);
      expect(req.set("home_css", Lists.newArrayList("/home.css"))).andReturn(req);
      expect(req.set("home_styles", "<link href=\"/home.css\" rel=\"stylesheet\">\n"))
          .andReturn(req);

      expect(req.set("home_js", Lists.newArrayList("/home.js"))).andReturn(req);
      expect(req.set("home_scripts", "<script src=\"/home.js\"></script>\n")).andReturn(req);

      unit.get(Route.Chain.class).next(req, unit.get(Response.class));
    }).run(unit -> {
      new Assets()
          .configure(unit.get(Env.class), conf, unit.get(Binder.class));
    }, unit -> {
      unit.captured(AssetVars.class).iterator().next().handle(unit.get(Request.class),
          unit.get(Response.class), unit.get(Route.Chain.class));
    });
  }

  @Test
  public void configureWithoutWatch() throws Exception {
    Config conf = ConfigFactory.empty()
        .withValue("application.path", ConfigValueFactory.fromAnyRef("/"))
        .withValue("assets.watch", ConfigValueFactory.fromAnyRef(false))
        .withValue("assets.etag", ConfigValueFactory.fromAnyRef(true))
        .withValue("assets.lastModified", ConfigValueFactory.fromAnyRef(true))
        .withValue("assets.cdn", ConfigValueFactory.fromAnyRef(""))
        .withValue("assets.cache.maxAge", ConfigValueFactory.fromAnyRef(-1));
    new MockUnit(Env.class, Binder.class, Request.class, Response.class,
        Route.Chain.class).expect(unit -> {
      AssetCompiler compiler = unit.constructor(AssetCompiler.class)
          .args(ClassLoader.class, Config.class)
          .build(Assets.class.getClassLoader(), conf);
      expect(compiler.patterns()).andReturn(Sets.newHashSet("/assets/**"));
      unit.registerMock(AssetCompiler.class, compiler);
    }).expect(unit -> {
      Definition assetVars = unit.mock(Definition.class);
      expect(assetVars.name("/assets/vars")).andReturn(assetVars);
      Router routes = unit.mock(Router.class);
      expect(routes.use(eq("*"), eq("*"), unit.capture(AssetVars.class))).andReturn(assetVars);

      Definition assetHandlerWithCompiler = unit.mock(Definition.class);
      expect(routes.get(eq("/assets/**"), isA(AssetHandlerWithCompiler.class)))
          .andReturn(assetHandlerWithCompiler);

      Env env = unit.get(Env.class);
      expect(env.router()).andReturn(routes);
    }).expect(unit -> {
      Env env = unit.get(Env.class);

      expect(env.name()).andReturn("dev");
    }).expect(unit -> {
      AssetCompiler compiler = unit.get(AssetCompiler.class);
      expect(compiler.fileset()).andReturn(Sets.newHashSet("home"));
      expect(compiler.styles("home")).andReturn(Lists.newArrayList("/home.css"));
      expect(compiler.scripts("home")).andReturn(Lists.newArrayList("/home.js"));

      Request req = unit.get(Request.class);
      expect(req.set("home_css", Lists.newArrayList("/home.css"))).andReturn(req);
      expect(req.set("home_styles", "<link href=\"/home.css\" rel=\"stylesheet\">\n"))
          .andReturn(req);

      expect(req.set("home_js", Lists.newArrayList("/home.js"))).andReturn(req);
      expect(req.set("home_scripts", "<script src=\"/home.js\"></script>\n")).andReturn(req);

      unit.get(Route.Chain.class).next(req, unit.get(Response.class));
    }).run(unit -> {
      new Assets()
          .configure(unit.get(Env.class), conf, unit.get(Binder.class));
    }, unit -> {
      unit.captured(AssetVars.class).iterator().next().handle(unit.get(Request.class),
          unit.get(Response.class), unit.get(Route.Chain.class));
    });
  }

  @Test
  public void configuredist() throws Exception {
    Config conf = ConfigFactory.empty()
        .withValue("application.env", ConfigValueFactory.fromAnyRef("prod"))
        .withValue("assets.etag", ConfigValueFactory.fromAnyRef(true))
        .withValue("application.path", ConfigValueFactory.fromAnyRef("/"))
        .withValue("assets.cdn", ConfigValueFactory.fromAnyRef(""))
        .withValue("assets.cache.maxAge", ConfigValueFactory.fromAnyRef("365d"))
        .withValue("assets.lastModified", ConfigValueFactory.fromAnyRef(true))
        .withValue("assets.watch", ConfigValueFactory.fromAnyRef(false));
    new MockUnit(Env.class, Config.class, Binder.class, Request.class, Response.class,
        Route.Chain.class, AssetCompiler.class).expect(unit -> {
      AssetCompiler compiler = unit.constructor(AssetCompiler.class)
          .args(ClassLoader.class, Config.class)
          .build(Assets.class.getClassLoader(), conf);
      expect(compiler.patterns()).andReturn(Sets.newHashSet("/assets/**"));
      unit.registerMock(AssetCompiler.class, compiler);
    }).expect(unit -> {
      Definition assetVars = unit.mock(Definition.class);
      expect(assetVars.name("/assets/vars")).andReturn(assetVars);
      Router routes = unit.mock(Router.class);
      expect(routes.use(eq("*"), eq("*"), unit.capture(AssetVars.class))).andReturn(assetVars);

      Definition assetHandlerWithCompiler = unit.mock(Definition.class);
      expect(routes.get(eq("/assets/**"), isA(AssetHandler.class)))
          .andReturn(assetHandlerWithCompiler);

      Env env = unit.get(Env.class);
      expect(env.router()).andReturn(routes);
    }).expect(unit -> {
      Env env = unit.get(Env.class);

      expect(env.name()).andReturn("prod");
    }).expect(unit -> {

      AssetCompiler compiler = unit.get(AssetCompiler.class);
      expect(compiler.fileset()).andReturn(Sets.newHashSet("home"));
      expect(compiler.styles("home")).andReturn(Lists.newArrayList("/home.css"));
      expect(compiler.scripts("home")).andReturn(Lists.newArrayList("/home.js"));

      Request req = unit.get(Request.class);
      expect(req.set("home_css", Lists.newArrayList("/home.css"))).andReturn(req);
      expect(req.set("home_styles", "<link href=\"/home.css\" rel=\"stylesheet\">\n"))
          .andReturn(req);

      expect(req.set("home_js", Lists.newArrayList("/home.js"))).andReturn(req);
      expect(req.set("home_scripts", "<script src=\"/home.js\"></script>\n")).andReturn(req);

      unit.get(Route.Chain.class).next(req, unit.get(Response.class));
    }).run(unit -> {
      new Assets()
          .configure(unit.get(Env.class), conf, unit.get(Binder.class));
    }, unit -> {
      unit.captured(AssetVars.class).iterator().next().handle(unit.get(Request.class),
          unit.get(Response.class), unit.get(Route.Chain.class));
    });
  }

  @SuppressWarnings("unchecked")
  @Test(expected = RuntimeException.class)
  public void configureFails() throws Exception {
    Config conf = ConfigFactory.empty();
    new MockUnit(Env.class, Config.class, Binder.class, Request.class, Response.class,
        Route.Chain.class, AssetCompiler.class).expect(unit -> {
      AssetCompiler compiler = unit.constructor(AssetCompiler.class)
          .args(ClassLoader.class, Config.class)
          .build(Assets.class.getClassLoader(), conf);
      expect(compiler.patterns()).andThrow(new RuntimeException());
      unit.registerMock(AssetCompiler.class, compiler);
    }).expect(unit -> {
      Binder binder = unit.get(Binder.class);

      LinkedBindingBuilder<Definition> varsLBB = unit.mock(LinkedBindingBuilder.class);
      varsLBB.toInstance(unit.capture(Route.Definition.class));

      LinkedBindingBuilder<Definition> handlerLBB = unit.mock(LinkedBindingBuilder.class);
      handlerLBB.toInstance(unit.capture(Route.Definition.class));

      Multibinder<Definition> mb = unit.mock(Multibinder.class);
      expect(mb.addBinding()).andReturn(varsLBB);
      expect(mb.addBinding()).andReturn(handlerLBB);

      unit.mockStatic(Multibinder.class);
      expect(Multibinder.newSetBinder(binder, Route.Definition.class)).andReturn(mb);
    }).expect(unit -> {
      Env env = unit.get(Env.class);

      expect(env.name()).andReturn("dev");
    }).expect(unit -> {

      AssetCompiler compiler = unit.get(AssetCompiler.class);
      expect(compiler.fileset()).andReturn(Sets.newHashSet("home"));
      expect(compiler.styles("home")).andReturn(Lists.newArrayList("/home.css"));
      expect(compiler.scripts("home")).andReturn(Lists.newArrayList("/home.js"));

      Request req = unit.get(Request.class);
      expect(req.set("home_css", Lists.newArrayList("/home.css"))).andReturn(req);
      expect(req.set("home_styles", "<link href=\"/home.css\" rel=\"stylesheet\">\n"))
          .andReturn(req);

      expect(req.set("home_js", Lists.newArrayList("/home.js"))).andReturn(req);
      expect(req.set("home_scripts", "<script src=\"/home.js\"></script>\n")).andReturn(req);

      unit.get(Route.Chain.class).next(req, unit.get(Response.class));
    }).run(unit -> {
      new Assets()
          .configure(unit.get(Env.class), conf, unit.get(Binder.class));
    }, unit -> {
      Filter handler = unit.captured(Route.Definition.class).iterator().next().filter();
      handler.handle(unit.get(Request.class), unit.get(Response.class),
          unit.get(Route.Chain.class));
    }, unit -> {
      Filter handler = unit.captured(Route.Definition.class).get(1).filter();
      assertTrue(handler instanceof AssetHandlerWithCompiler);
    });
  }

}
