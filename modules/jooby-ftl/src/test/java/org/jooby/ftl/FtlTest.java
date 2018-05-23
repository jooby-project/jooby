package org.jooby.ftl;

import com.google.inject.Binder;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import freemarker.cache.CacheStorage;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.NullCacheStorage;
import freemarker.core.HTMLOutputFormat;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.template.Version;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import org.jooby.Env;
import org.jooby.Renderer;
import org.jooby.internal.ftl.Engine;
import org.jooby.internal.ftl.GuavaCacheStorage;
import org.jooby.internal.ftl.XssDirective;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Ftl.class, Configuration.class, ClassTemplateLoader.class, Multibinder.class,
    XssDirective.class})
public class FtlTest {

  private Block xss = unit -> {
    Env env = unit.get(Env.class);
    XssDirective xss = unit.constructor(XssDirective.class)
        .build(env);
    unit.registerMock(XssDirective.class, xss);
  };

  @SuppressWarnings("unchecked")
  @Test
  public void defaults() throws Exception {
    String prefix = "/";
    String suffix = ".html";

    Properties props = new Properties();
    props.setProperty("default_encoding", "UTF-8");

    Config freemarker = ConfigFactory.empty()
        .withValue("default_encoding", ConfigValueFactory.fromAnyRef("UTF-8"));

    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getConfig("freemarker")).andReturn(freemarker);
        })
        .expect(xss)
        .expect(defaults(prefix, suffix, props))
        .expect(env("dev"))
        .run(unit -> {
          new Ftl()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  private Block defaults(String prefix, String suffix, Properties props) {
    return defaults(prefix, suffix, props, NullCacheStorage.class);
  }

  private Block defaults(String prefix, String suffix, Properties props,
      Class<? extends CacheStorage> cacheStorage) {
    return unit -> {
      ClassTemplateLoader loader = unit.mockConstructor(ClassTemplateLoader.class,
          new Class[]{ClassLoader.class, String.class}, Ftl.class.getClassLoader(), prefix);

      Configuration config = unit.mockConstructor(Configuration.class,
          new Class[]{Version.class}, Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
      unit.registerMock(Configuration.class, config);

      config.setSettings(props);
      config.setTemplateLoader(loader);
      config.setCacheStorage(isA(cacheStorage));
      config.setOutputFormat(HTMLOutputFormat.INSTANCE);

      AnnotatedBindingBuilder<Configuration> configBB = unit
          .mock(AnnotatedBindingBuilder.class);
      configBB.toInstance(config);

      Binder binder = unit.get(Binder.class);
      expect(binder.bind(Configuration.class)).andReturn(configBB);

      XssDirective xss = unit.get(XssDirective.class);
      Engine engine = unit.mockConstructor(
          Engine.class, new Class[]{Configuration.class, String.class, XssDirective.class},
          config, suffix, xss);

      LinkedBindingBuilder<Renderer> ffLBB = unit.mock(LinkedBindingBuilder.class);
      ffLBB.toInstance(engine);

      Multibinder<Renderer> formatter = unit.mock(Multibinder.class);
      expect(formatter.addBinding()).andReturn(ffLBB);

      unit.mockStatic(Multibinder.class);
      expect(Multibinder.newSetBinder(binder, Renderer.class)).andReturn(formatter);

    };
  }

  @SuppressWarnings("unchecked")
  @Test
  public void defaultsNoDev() throws Exception {
    String prefix = "/";
    String suffix = ".html";

    Properties props = new Properties();
    props.setProperty("default_encoding", "UTF-8");

    Config freemarker = ConfigFactory.empty()
        .withValue("default_encoding", ConfigValueFactory.fromAnyRef("UTF-8"));

    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);

          expect(config.getConfig("freemarker")).andReturn(freemarker);
          expect(config.getString("freemarker.cache")).andReturn("maximumSize=100").times(2);
        })
        .expect(xss)
        .expect(defaults(prefix, suffix, props, GuavaCacheStorage.class))
        .expect(env("prod"))
        .run(unit -> {
          new Ftl()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test(expected = TemplateException.class)
  public void err() throws Exception {

    Properties props = new Properties();
    props.setProperty("default_encoding", "UTF-8");

    Config freemarker = ConfigFactory.empty()
        .withValue("default_encoding", ConfigValueFactory.fromAnyRef("UTF-8"));

    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getConfig("freemarker")).andReturn(freemarker);
        })
        .expect(unit -> {
          Configuration config = unit.mockConstructor(Configuration.class,
              new Class[]{Version.class}, Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);

          config.setSettings(props);
          expectLastCall().andThrow(new TemplateException("intentional err", null));

        })
        .expect(env("dev"))
        .run(unit -> {
          new Ftl()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void withPrefix() throws Exception {
    String prefix = "/x";
    String suffix = ".html";

    Properties props = new Properties();
    props.setProperty("default_encoding", "UTF-8");

    Config freemarker = ConfigFactory.empty()
        .withValue("default_encoding", ConfigValueFactory.fromAnyRef("UTF-8"));

    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getConfig("freemarker")).andReturn(freemarker);
        })
        .expect(xss)
        .expect(defaults(prefix, suffix, props))
        .expect(env("dev"))
        .run(unit -> {
          new Ftl(prefix)
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  private Block env(String name) {
    return unit -> {
      Env env = unit.get(Env.class);
      expect(env.name()).andReturn(name);
    };
  }

  @SuppressWarnings("unchecked")
  @Test
  public void withPrefixAndSuffix() throws Exception {
    String prefix = "/x";
    String suffix = ".ftl";

    Properties props = new Properties();
    props.setProperty("default_encoding", "UTF-8");

    Config freemarker = ConfigFactory.empty()
        .withValue("default_encoding", ConfigValueFactory.fromAnyRef("UTF-8"));

    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getConfig("freemarker")).andReturn(freemarker);
        })
        .expect(xss)
        .expect(defaults(prefix, suffix, props))
        .expect(env("dev"))
        .run(unit -> {
          new Ftl(prefix, suffix)
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void withBiConsumerConfigurer() throws Exception {
    String prefix = "/x";
    String suffix = ".ftl";

    Properties props = new Properties();
    props.setProperty("default_encoding", "UTF-8");

    Config freemarker = ConfigFactory.empty()
        .withValue("default_encoding", ConfigValueFactory.fromAnyRef("UTF-8"));

    new MockUnit(Env.class, Config.class, Binder.class, BiConsumer.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getConfig("freemarker")).andReturn(freemarker);
        })
        .expect(xss)
        .expect(defaults(prefix, suffix, props))
        .expect(env("dev"))
        .expect(unit -> {
          unit.get(BiConsumer.class).accept(unit.get(Configuration.class), unit.get(Config.class));
        })
        .run(unit -> {
          new Ftl(prefix, suffix)
              .doWith(unit.get(BiConsumer.class))
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void withConfigurer() throws Exception {
    String prefix = "/x";
    String suffix = ".ftl";

    Properties props = new Properties();
    props.setProperty("default_encoding", "UTF-8");

    Config freemarker = ConfigFactory.empty()
        .withValue("default_encoding", ConfigValueFactory.fromAnyRef("UTF-8"));

    new MockUnit(Env.class, Config.class, Binder.class, BiConsumer.class, Consumer.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getConfig("freemarker")).andReturn(freemarker);
        })
        .expect(xss)
        .expect(defaults(prefix, suffix, props))
        .expect(env("dev"))
        .expect(unit -> {
          unit.get(Consumer.class).accept(unit.get(Configuration.class));
        })
        .run(unit -> {
          new Ftl(prefix, suffix)
              .doWith(unit.get(Consumer.class))
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void config() throws Exception {
    Config config = new Ftl().config();
    assertEquals("org/jooby/ftl/freemarker.conf", config.root().origin().resource());
  }

}
