package org.jooby.assets;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Binder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import static org.easymock.EasyMock.anyLong;
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
import org.jooby.funzy.Throwing;
import org.jooby.handlers.AssetHandler;
import org.jooby.internal.assets.FileSystemAssetHandler;
import org.jooby.internal.assets.AssetVars;
import org.jooby.internal.assets.LiveCompiler;
import org.jooby.test.MockUnit;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Assets.class, AssetCompiler.class, Multibinder.class, LiveCompiler.class,
    System.class})
public class AssetsTest {

  private MockUnit.Block assetsVars = unit -> {
    Definition assetVars = unit.mock(Definition.class);
    expect(assetVars.name("/assets/vars")).andReturn(assetVars);

    Router routes = unit.get(Router.class);
    expect(routes.use(eq("*"), eq("*"), unit.capture(AssetVars.class))).andReturn(assetVars);
  };

  private MockUnit.Block handlerWithCompiler = unit -> {
    Router routes = unit.get(Router.class);

    Definition assetHandlerWithCompiler = unit.mock(Definition.class);
    expect(routes.get(eq("/assets/**"), isA(FileSystemAssetHandler.class)))
        .andReturn(assetHandlerWithCompiler);
  };

  private MockUnit.Block progressBar = unit -> {
    AssetCompiler compiler = unit.get(AssetCompiler.class);
    compiler.setProgressBar(unit.capture(BiConsumer.class));
  };

  private MockUnit.Block handleRequest = unit -> {
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
  };

  private MockUnit.Block runAssetVars = unit -> {
    unit.captured(AssetVars.class).get(0)
        .handle(unit.get(Request.class), unit.get(Response.class), unit.get(Route.Chain.class));
  };

  private MockUnit.Block assetCompilerStop = unit -> {
    unit.captured(Throwing.Runnable.class).forEach(Throwing.Runnable::run);
  };

  @Test
  public void configure() throws Exception {
    Config conf = ConfigFactory.empty()
        .withValue("user.dir", ConfigValueFactory.fromAnyRef(System.getProperty("user.dir")))
        .withValue("application.path", ConfigValueFactory.fromAnyRef("/path"))
        .withValue("assets.etag", ConfigValueFactory.fromAnyRef(true))
        .withValue("assets.outputDir",
            ConfigValueFactory.fromAnyRef(Paths.get("target", "output").toString()))
        .withValue("assets.lastModified", ConfigValueFactory.fromAnyRef(true))
        .withValue("assets.cdn", ConfigValueFactory.fromAnyRef(""))
        .withValue("assets.cache.maxAge", ConfigValueFactory.fromAnyRef(-1));
    new MockUnit(Env.class, Binder.class, Router.class, Request.class, Response.class,
        Route.Chain.class)
        .expect(assetCompiler(conf))
        .expect(assetsVars)
        .expect(handlerWithCompiler)
        .expect(progressBar)
        .expect(env("dev"))
        .expect(liveCompiler(conf, true))
        .expect(print("dev", conf))
        .expect(handleRequest)
        .run(unit -> {
          new Assets()
              .configure(unit.get(Env.class), conf, unit.get(Binder.class));
        }, runAssetVars, assetCompilerStop);
  }

  @Test
  public void configureRestart() throws Exception {
    Config conf = ConfigFactory.empty()
        .withValue("user.dir", ConfigValueFactory.fromAnyRef(System.getProperty("user.dir")))
        .withValue("application.path", ConfigValueFactory.fromAnyRef("/path"))
        .withValue("assets.etag", ConfigValueFactory.fromAnyRef(true))
        .withValue("assets.outputDir",
            ConfigValueFactory.fromAnyRef(Paths.get("target", "output").toString()))
        .withValue("assets.lastModified", ConfigValueFactory.fromAnyRef(true))
        .withValue("assets.cdn", ConfigValueFactory.fromAnyRef(""))
        .withValue("assets.cache.maxAge", ConfigValueFactory.fromAnyRef(-1));
    new MockUnit(Env.class, Binder.class, Router.class, Request.class, Response.class,
        Route.Chain.class)
        .expect(assetCompiler(conf))
        .expect(assetsVars)
        .expect(handlerWithCompiler)
        .expect(env("dev"))
        .expect(unit -> {
          unit.mockStatic(System.class);
          expect(System.getProperty("joobyRun.counter", "0")).andReturn("1");
        })
        .expect(liveCompiler(conf, false))
        .expect(handleRequest)
        .run(unit -> {
          new Assets()
              .configure(unit.get(Env.class), conf, unit.get(Binder.class));
        }, runAssetVars, assetCompilerStop);
  }

  @Test
  public void configureProdDist() throws Exception {
    Config conf = ConfigFactory.empty()
        .withValue("user.dir", ConfigValueFactory.fromAnyRef(System.getProperty("user.dir")))
        .withValue("application.env", ConfigValueFactory.fromAnyRef("prod"))
        .withValue("assets.etag", ConfigValueFactory.fromAnyRef(true))
        .withValue("application.path", ConfigValueFactory.fromAnyRef("/"))
        .withValue("assets.cdn", ConfigValueFactory.fromAnyRef(""))
        .withValue("assets.cache.maxAge", ConfigValueFactory.fromAnyRef("365d"))
        .withValue("assets.lastModified", ConfigValueFactory.fromAnyRef(true))
        .withValue("assets.watch", ConfigValueFactory.fromAnyRef(false));
    new MockUnit(Env.class, Config.class, Binder.class, Request.class, Response.class,
        Route.Chain.class, Router.class)
        .expect(assetCompiler(conf))
        .expect(assetsVars)
        .expect(unit -> {
          Router routes = unit.get(Router.class);

          Definition assetHandlerWithCompiler = unit.mock(Definition.class);
          expect(routes.get(eq("/assets/**"), isA(AssetHandler.class)))
              .andReturn(assetHandlerWithCompiler);

          Env env = unit.get(Env.class);
          expect(env.router()).andReturn(routes);
        })
        .expect(env("prod"))
        .run(unit -> {
          new Assets()
              .configure(unit.get(Env.class), conf, unit.get(Binder.class));
        }, assetCompilerStop);
  }

  private MockUnit.Block print(String env, Config conf) {
    return unit -> {
      AssetCompiler compiler = unit.get(AssetCompiler.class);
      expect(compiler.summary(isA(Map.class), eq(Paths.get(conf.getString("assets.outputDir"))), eq(env),
          anyLong())).andReturn("Summary:");
    };
  }

  private MockUnit.Block assetCompiler(Config conf) {
    return unit -> {
      AssetCompiler compiler = unit.constructor(AssetCompiler.class)
          .args(ClassLoader.class, Config.class)
          .build(Assets.class.getClassLoader(), conf);
      compiler.stop();
      expect(compiler.patterns()).andReturn(Sets.newHashSet("/assets/**"));

      unit.registerMock(AssetCompiler.class, compiler);

      Env env = unit.get(Env.class);
      expect(env.onStop(unit.capture(Throwing.Runnable.class))).andReturn(env);
    };
  }

  private MockUnit.Block env(String name) {
    return unit -> {
      Env env = unit.get(Env.class);
      expect(env.name()).andReturn(name);
    };
  }

  private MockUnit.Block liveCompiler(Config conf, boolean firstRun) {
    return unit -> {
      AssetCompiler compiler = unit.get(AssetCompiler.class);

      Router routes = unit.get(Router.class);

      Definition liveCompilerRoute = unit.mock(Definition.class);
      expect(liveCompilerRoute.name("/assets/compiler")).andReturn(liveCompilerRoute);

      LiveCompiler liveCompiler = unit.constructor(LiveCompiler.class)
          .args(AssetCompiler.class, Path.class)
          .build(compiler, Paths.get(conf.getString("assets.outputDir")));
      CompletableFuture<Map<String, List<File>>> future = CompletableFuture
          .completedFuture(ImmutableMap.of("index", ImmutableList.of(new File("target/index.js"))));
      expect(routes.use(eq("*"), eq("*"), eq(liveCompiler))).andReturn(liveCompilerRoute);
      if (firstRun) {
        expect(liveCompiler.sync()).andReturn(future);
      }
      liveCompiler.watch();
      liveCompiler.stop();

      Env env = unit.get(Env.class);
      expect(env.router()).andReturn(routes);
      expect(env.onStarted(unit.capture(Throwing.Runnable.class))).andReturn(env);
      expect(env.onStop(unit.capture(Throwing.Runnable.class))).andReturn(env);
    };
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
      assertTrue(handler instanceof FileSystemAssetHandler);
    });
  }

}
