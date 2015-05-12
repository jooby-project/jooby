package org.jooby.ftl;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;

import java.util.Properties;
import java.util.function.BiConsumer;

import org.jooby.Env;
import org.jooby.MockUnit;
import org.jooby.Renderer;
import org.jooby.View;
import org.jooby.internal.ftl.Engine;
import org.jooby.internal.ftl.GuavaCacheStorage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.NullCacheStorage;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.template.Version;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Ftl.class, Configuration.class, ClassTemplateLoader.class, Multibinder.class })
public class FtlTest {

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
        .expect(unit -> {
          ClassTemplateLoader loader = unit.mockConstructor(ClassTemplateLoader.class,
              new Class[]{ClassLoader.class, String.class }, Ftl.class.getClassLoader(), prefix);

          Configuration config = unit.mockConstructor(Configuration.class,
              new Class[]{Version.class }, Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);

          config.setSettings(props);
          config.setTemplateLoader(loader);
          config.setCacheStorage(NullCacheStorage.INSTANCE);

          AnnotatedBindingBuilder<Configuration> configBB =
              unit.mock(AnnotatedBindingBuilder.class);
          configBB.toInstance(config);

          Binder binder = unit.get(Binder.class);
          expect(binder.bind(Configuration.class)).andReturn(configBB);

          Engine engine = unit.mockConstructor(
              Engine.class, new Class[]{Configuration.class, String.class, String.class },
              config, prefix, suffix);
          expect(engine.name()).andReturn("ftl");

          LinkedBindingBuilder<Renderer> ffLBB = unit.mock(LinkedBindingBuilder.class);
          ffLBB.toInstance(engine);

          Multibinder<Renderer> formatter = unit.mock(Multibinder.class);
          expect(formatter.addBinding()).andReturn(ffLBB);

          unit.mockStatic(Multibinder.class);
          expect(Multibinder.newSetBinder(binder, Renderer.class)).andReturn(formatter);

          LinkedBindingBuilder<View.Engine> eLBB = unit.mock(LinkedBindingBuilder.class);
          eLBB.toInstance(engine);

          expect(binder.bind(Key.get(View.Engine.class, Names.named("ftl")))).andReturn(eLBB);

        })
        .expect(unit -> {
          Env env = unit.get(Env.class);
          expect(env.name()).andReturn("dev");
        })
        .run(unit -> {
          new Ftl()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
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
        .expect(unit -> {
          ClassTemplateLoader loader = unit.mockConstructor(ClassTemplateLoader.class,
              new Class[]{ClassLoader.class, String.class }, Ftl.class.getClassLoader(), prefix);

          Configuration config = unit.mockConstructor(Configuration.class,
              new Class[]{Version.class }, Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);

          config.setSettings(props);
          config.setTemplateLoader(loader);
          config.setCacheStorage(isA(GuavaCacheStorage.class));

          AnnotatedBindingBuilder<Configuration> configBB =
              unit.mock(AnnotatedBindingBuilder.class);
          configBB.toInstance(config);

          Binder binder = unit.get(Binder.class);
          expect(binder.bind(Configuration.class)).andReturn(configBB);

          Engine engine = unit.mockConstructor(
              Engine.class, new Class[]{Configuration.class, String.class, String.class },
              config, prefix, suffix);
          expect(engine.name()).andReturn("ftl");

          LinkedBindingBuilder<Renderer> ffLBB = unit.mock(LinkedBindingBuilder.class);
          ffLBB.toInstance(engine);

          Multibinder<Renderer> formatter = unit.mock(Multibinder.class);
          expect(formatter.addBinding()).andReturn(ffLBB);

          unit.mockStatic(Multibinder.class);
          expect(Multibinder.newSetBinder(binder, Renderer.class)).andReturn(formatter);

          LinkedBindingBuilder<View.Engine> eLBB = unit.mock(LinkedBindingBuilder.class);
          eLBB.toInstance(engine);

          expect(binder.bind(Key.get(View.Engine.class, Names.named("ftl")))).andReturn(eLBB);

        })
        .expect(unit -> {
          Env env = unit.get(Env.class);
          expect(env.name()).andReturn("prod");
        })
        .run(unit -> {
          new Ftl()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test(expected = IllegalStateException.class)
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
              new Class[]{Version.class }, Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);

          config.setSettings(props);
          expectLastCall().andThrow(new TemplateException("intentional err", null));

        })
        .expect(unit -> {
          Env env = unit.get(Env.class);
          expect(env.name()).andReturn("dev");
        })
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
        .expect(unit -> {
          ClassTemplateLoader loader = unit.mockConstructor(ClassTemplateLoader.class,
              new Class[]{ClassLoader.class, String.class }, Ftl.class.getClassLoader(), prefix);

          Configuration config = unit.mockConstructor(Configuration.class,
              new Class[]{Version.class }, Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);

          config.setSettings(props);
          config.setTemplateLoader(loader);
          config.setCacheStorage(NullCacheStorage.INSTANCE);

          AnnotatedBindingBuilder<Configuration> configBB =
              unit.mock(AnnotatedBindingBuilder.class);
          configBB.toInstance(config);

          Binder binder = unit.get(Binder.class);
          expect(binder.bind(Configuration.class)).andReturn(configBB);

          Engine engine = unit.mockConstructor(
              Engine.class, new Class[]{Configuration.class, String.class, String.class },
              config, prefix, suffix);
          expect(engine.name()).andReturn("ftl");

          LinkedBindingBuilder<Renderer> ffLBB = unit.mock(LinkedBindingBuilder.class);
          ffLBB.toInstance(engine);

          Multibinder<Renderer> formatter = unit.mock(Multibinder.class);
          expect(formatter.addBinding()).andReturn(ffLBB);

          unit.mockStatic(Multibinder.class);
          expect(Multibinder.newSetBinder(binder, Renderer.class)).andReturn(formatter);

          LinkedBindingBuilder<View.Engine> eLBB = unit.mock(LinkedBindingBuilder.class);
          eLBB.toInstance(engine);

          expect(binder.bind(Key.get(View.Engine.class, Names.named("ftl")))).andReturn(eLBB);

        })
        .expect(unit -> {
          Env env = unit.get(Env.class);
          expect(env.name()).andReturn("dev");
        })
        .run(unit -> {
          new Ftl(prefix)
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
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
        .expect(unit -> {
          ClassTemplateLoader loader = unit.mockConstructor(ClassTemplateLoader.class,
              new Class[]{ClassLoader.class, String.class }, Ftl.class.getClassLoader(), prefix);

          Configuration config = unit.mockConstructor(Configuration.class,
              new Class[]{Version.class }, Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);

          config.setSettings(props);
          config.setTemplateLoader(loader);
          config.setCacheStorage(NullCacheStorage.INSTANCE);

          AnnotatedBindingBuilder<Configuration> configBB =
              unit.mock(AnnotatedBindingBuilder.class);
          configBB.toInstance(config);

          Binder binder = unit.get(Binder.class);
          expect(binder.bind(Configuration.class)).andReturn(configBB);

          Engine engine = unit.mockConstructor(
              Engine.class, new Class[]{Configuration.class, String.class, String.class },
              config, prefix, suffix);
          expect(engine.name()).andReturn("ftl");

          LinkedBindingBuilder<Renderer> ffLBB = unit.mock(LinkedBindingBuilder.class);
          ffLBB.toInstance(engine);

          Multibinder<Renderer> formatter = unit.mock(Multibinder.class);
          expect(formatter.addBinding()).andReturn(ffLBB);

          unit.mockStatic(Multibinder.class);
          expect(Multibinder.newSetBinder(binder, Renderer.class)).andReturn(formatter);

          LinkedBindingBuilder<View.Engine> eLBB = unit.mock(LinkedBindingBuilder.class);
          eLBB.toInstance(engine);

          expect(binder.bind(Key.get(View.Engine.class, Names.named("ftl")))).andReturn(eLBB);

        })
        .expect(unit -> {
          Env env = unit.get(Env.class);
          expect(env.name()).andReturn("dev");
        })
        .run(unit -> {
          new Ftl(prefix, suffix)
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

    new MockUnit(Env.class, Config.class, Binder.class, BiConsumer.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getConfig("freemarker")).andReturn(freemarker);
        })
        .expect(unit -> {
          ClassTemplateLoader loader = unit.mockConstructor(ClassTemplateLoader.class,
              new Class[]{ClassLoader.class, String.class }, Ftl.class.getClassLoader(), prefix);

          Configuration config = unit.mockConstructor(Configuration.class,
              new Class[]{Version.class }, Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);

          unit.get(BiConsumer.class).accept(config, unit.get(Config.class));

          config.setSettings(props);
          config.setTemplateLoader(loader);
          config.setCacheStorage(NullCacheStorage.INSTANCE);

          AnnotatedBindingBuilder<Configuration> configBB =
              unit.mock(AnnotatedBindingBuilder.class);
          configBB.toInstance(config);

          Binder binder = unit.get(Binder.class);
          expect(binder.bind(Configuration.class)).andReturn(configBB);

          Engine engine = unit.mockConstructor(
              Engine.class, new Class[]{Configuration.class, String.class, String.class },
              config, prefix, suffix);
          expect(engine.name()).andReturn("ftl");

          LinkedBindingBuilder<Renderer> ffLBB = unit.mock(LinkedBindingBuilder.class);
          ffLBB.toInstance(engine);

          Multibinder<Renderer> formatter = unit.mock(Multibinder.class);
          expect(formatter.addBinding()).andReturn(ffLBB);

          unit.mockStatic(Multibinder.class);
          expect(Multibinder.newSetBinder(binder, Renderer.class)).andReturn(formatter);

          LinkedBindingBuilder<View.Engine> eLBB = unit.mock(LinkedBindingBuilder.class);
          eLBB.toInstance(engine);

          expect(binder.bind(Key.get(View.Engine.class, Names.named("ftl")))).andReturn(eLBB);

        })
        .expect(unit -> {
          Env env = unit.get(Env.class);
          expect(env.name()).andReturn("dev");
        })
        .run(unit -> {
          new Ftl(prefix, suffix)
              .doWith(unit.get(BiConsumer.class))
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void config() throws Exception {
    Config config = new Ftl().config();
    assertEquals("org/jooby/ftl/freemarker.conf", config.root().origin().resource());
  }

}
