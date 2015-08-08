package org.jooby.less;

import static org.easymock.EasyMock.expect;

import org.jooby.Env;
import org.jooby.Route;
import org.jooby.Route.Definition;
import org.jooby.internal.less.ForwardingLessCompiler;
import org.jooby.internal.less.LessHandler;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.github.sommeri.less4j.LessCompiler;
import com.github.sommeri.less4j.LessCompiler.Configuration;
import com.github.sommeri.less4j.LessCompiler.SourceMapConfiguration;
import com.github.sommeri.less4j.core.DefaultLessCompiler;
import com.google.inject.Binder;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Less.class, LessHandler.class, Configuration.class, DefaultLessCompiler.class,
    Multibinder.class, Route.Definition.class })
public class LessTest {

  private Block dev = unit -> {
    Env env = unit.get(Env.class);
    expect(env.name()).andReturn("dev");
  };

  private Block nodev = unit -> {
    Env env = unit.get(Env.class);
    expect(env.name()).andReturn("prod");
  };

  private Block config = unit -> {
    Config conf = unit.get(Config.class);
    expect(conf.getConfig("less")).andReturn(config());
  };

  @SuppressWarnings("unchecked")
  private Block lessCompiler = unit -> {
    Configuration options = unit.get(Configuration.class);
    DefaultLessCompiler lessCompiler = unit.constructor(DefaultLessCompiler.class).build();
    ForwardingLessCompiler fwdLessCompiler = unit.constructor(ForwardingLessCompiler.class)
        .args(LessCompiler.class, Configuration.class)
        .build(lessCompiler, options);

    unit.registerMock(LessCompiler.class, fwdLessCompiler);

    AnnotatedBindingBuilder<LessCompiler> abbLC = unit.mock(AnnotatedBindingBuilder.class);
    abbLC.toInstance(fwdLessCompiler);

    Binder binder = unit.get(Binder.class);
    expect(binder.bind(LessCompiler.class)).andReturn(abbLC);
  };

  private Block lessHandler = unit -> {
    Config conf = unit.get(Config.class);
    expect(conf.getString("assets.cdn")).andReturn("");
    expect(conf.getBoolean("assets.etag")).andReturn(true);

    LessHandler handler = unit.constructor(LessHandler.class)
        .args(String.class, LessCompiler.class)
        .build("/", unit.get(LessCompiler.class));

    expect(handler.cdn("")).andReturn(handler);
    expect(handler.etag(true)).andReturn(handler);

    Route.Definition route = unit.constructor(Route.Definition.class)
        .args(String.class, String.class, Route.Filter.class)
        .build("GET", "/css/**", handler);

    unit.registerMock(Route.Definition.class, route);
  };

  @SuppressWarnings("unchecked")
  private Block bind = unit -> {
    unit.mockStatic(Multibinder.class);

    LinkedBindingBuilder<Definition> lbbRD = unit.mock(LinkedBindingBuilder.class);
    lbbRD.toInstance(unit.get(Route.Definition.class));

    Multibinder<Definition> mbinder = unit.mock(Multibinder.class);
    expect(Multibinder.newSetBinder(unit.get(Binder.class), Route.Definition.class))
        .andReturn(mbinder);
    expect(mbinder.addBinding()).andReturn(lbbRD);
  };

  public Config config() {
    return new Less("/")
        .config()
        .getConfig("less")
        .withFallback(
            ConfigFactory.empty()
                .withValue("application.charset", ConfigValueFactory.fromAnyRef("utf-8"))
        ).resolve();
  }

  @Test
  public void configure() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(dev)
        .expect(config)
        .expect(defConfig(false, true))
        .expect(lessCompiler)
        .expect(lessHandler)
        .expect(bind)
        .run(unit -> {
          new Less("/css/**")
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });

  }

  @Test
  public void withConfigurerCallback() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(dev)
        .expect(config)
        .expect(defConfig(false, true))
        .expect(lessCompiler)
        .expect(lessHandler)
        .expect(bind)
        .expect(unit -> {
          Configuration conf = unit.get(Configuration.class);
          expect(conf.setCompressing(true)).andReturn(conf);
        })
        .run(unit -> {
          new Less("/css/**")
              .doWith(conf -> {
                conf.setCompressing(true);
              })
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });

  }

  @Test
  public void shouldCompressOnNoDev() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(nodev)
        .expect(config)
        .expect(defConfig(true, false))
        .expect(lessCompiler)
        .expect(lessHandler)
        .expect(bind)
        .run(unit -> {
          new Less("/css/**")
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });

  }

  @Test
  public void shouldUseCompressWhenSet() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(nodev)
        .expect(unit -> {
          Config conf = unit.get(Config.class);
          expect(conf.getConfig("less")).andReturn(
              config().withValue("compressing", ConfigValueFactory.fromAnyRef(true)));
        })
        .expect(defConfig(true, false))
        .expect(lessCompiler)
        .expect(lessHandler)
        .expect(bind)
        .run(unit -> {
          new Less("/css/**")
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void shouldUseLinkSourceMapWhenSet() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(nodev)
        .expect(unit -> {
          Config conf = unit.get(Config.class);
          expect(conf.getConfig("less")).andReturn(
              config().withValue("sourceMap.linkSourceMap", ConfigValueFactory.fromAnyRef(true)));
        })
        .expect(defConfig(true, true))
        .expect(lessCompiler)
        .expect(lessHandler)
        .expect(bind)
        .run(unit -> {
          new Less("/css/**")
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  private Block defConfig(final boolean compressing, final boolean linkSourceMap) {
    return unit -> {
      SourceMapConfiguration sourceMap = unit.mock(SourceMapConfiguration.class);
      expect(sourceMap.setEncodingCharset("utf-8")).andReturn(sourceMap);
      expect(sourceMap.setIncludeSourcesContent(false)).andReturn(sourceMap);
      expect(sourceMap.setInline(false)).andReturn(sourceMap);
      expect(sourceMap.setRelativizePaths(true)).andReturn(sourceMap);
      expect(sourceMap.setLinkSourceMap(linkSourceMap)).andReturn(sourceMap);

      Configuration configuration = unit.mockConstructor(Configuration.class);
      expect(configuration.setCompressing(compressing)).andReturn(configuration);
      expect(configuration.getSourceMapConfiguration()).andReturn(sourceMap);
      unit.registerMock(Configuration.class, configuration);
    };
  }

}
