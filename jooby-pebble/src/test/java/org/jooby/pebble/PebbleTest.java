package org.jooby.pebble;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertNotNull;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;

import org.jooby.Env;
import org.jooby.Renderer;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Binder;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.cache.BaseTagCacheKey;
import com.mitchellbosecke.pebble.loader.ClasspathLoader;
import com.mitchellbosecke.pebble.template.PebbleTemplate;
import com.typesafe.config.Config;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Pebble.class, PebbleEngine.Builder.class, ClasspathLoader.class,
    Multibinder.class, CacheBuilder.class })
public class PebbleTest {

  private Block newEngine = unit -> {
    PebbleEngine.Builder pebble = unit.constructor(PebbleEngine.Builder.class)
        .build();

    expect(pebble.cacheActive(true)).andReturn(pebble);
    expect(pebble.loader(unit.get(ClasspathLoader.class))).andReturn(pebble);
    expect(pebble.extension(isA(XssExt.class))).andReturn(pebble);

    unit.registerMock(PebbleEngine.Builder.class, pebble);
  };

  private Block defLoader = unit -> {
    ClasspathLoader loader = unit.constructor(ClasspathLoader.class).build();
    loader.setPrefix(null);
    loader.setSuffix(".html");
    unit.registerMock(ClasspathLoader.class, loader);
  };

  private Block build = unit -> {
    PebbleEngine.Builder pebble = unit.get(PebbleEngine.Builder.class);
    expect(pebble.build()).andReturn(unit.get(PebbleEngine.class));
  };

  @SuppressWarnings("unchecked")
  private Block bindEngine = unit -> {
    AnnotatedBindingBuilder<PebbleEngine> peABB = unit.mock(AnnotatedBindingBuilder.class);
    peABB.toInstance(unit.get(PebbleEngine.class));

    Binder binder = unit.get(Binder.class);
    expect(binder.bind(PebbleEngine.class)).andReturn(peABB);
  };

  @SuppressWarnings("unchecked")
  private Block renderer = unit -> {
    PebbleRenderer renderer = unit.constructor(PebbleRenderer.class)
        .args(PebbleEngine.class)
        .build(unit.get(PebbleEngine.class));
    unit.mockStatic(Multibinder.class);

    LinkedBindingBuilder<Renderer> rLBB = unit.mock(LinkedBindingBuilder.class);
    rLBB.toInstance(renderer);

    Multibinder<Renderer> rmb = unit.mock(Multibinder.class);
    expect(rmb.addBinding()).andReturn(rLBB);

    expect(Multibinder.newSetBinder(unit.get(Binder.class), Renderer.class)).andReturn(rmb);
  };

  private Block cacheStatic = unit -> {
    unit.mockStatic(CacheBuilder.class);
  };

  @Test
  public void basic() throws Exception {
    Locale locale = Locale.getDefault();
    new MockUnit(Env.class, Config.class, Binder.class, PebbleEngine.class)
        .expect(defLoader)
        .expect(newEngine)
        .expect(env("dev", locale))
        .expect(cacheStatic)
        .expect(cache("pebble.cache", null))
        .expect(cache(0))
        .expect(cache("pebble.tagCache", null))
        .expect(tagCache(0))
        .expect(locale(locale))
        .expect(build)
        .expect(bindEngine)
        .expect(renderer)
        .run(unit -> {
          new Pebble()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void proddef() throws Exception {
    Locale locale = Locale.getDefault();
    new MockUnit(Env.class, Config.class, Binder.class, PebbleEngine.class)
        .expect(defLoader)
        .expect(newEngine)
        .expect(env("prod", locale))
        .expect(cacheStatic)
        .expect(cache("pebble.cache", null))
        .expect(cache(100))
        .expect(cache("pebble.tagCache", null))
        .expect(tagCache(100))
        .expect(locale(locale))
        .expect(build)
        .expect(bindEngine)
        .expect(renderer)
        .run(unit -> {
          new Pebble()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void doWithFull() throws Exception {
    Locale locale = Locale.getDefault();
    CountDownLatch latch = new CountDownLatch(1);
    new MockUnit(Env.class, Config.class, Binder.class, PebbleEngine.class)
        .expect(defLoader)
        .expect(newEngine)
        .expect(env("dev", locale))
        .expect(cacheStatic)
        .expect(cache("pebble.cache", null))
        .expect(cache(0))
        .expect(cache("pebble.tagCache", null))
        .expect(tagCache(0))
        .expect(locale(locale))
        .expect(build)
        .expect(bindEngine)
        .expect(renderer)
        .run(unit -> {
          new Pebble(".html").doWith((b, c) -> {
            assertNotNull(b);
            assertNotNull(c);
            latch.countDown();
          })
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
    latch.await();
  }

  @Test
  public void doWith1() throws Exception {
    Locale locale = Locale.getDefault();
    CountDownLatch latch = new CountDownLatch(1);
    new MockUnit(Env.class, Config.class, Binder.class, PebbleEngine.class)
        .expect(defLoader)
        .expect(newEngine)
        .expect(env("dev", locale))
        .expect(cacheStatic)
        .expect(cache("pebble.cache", null))
        .expect(cache(0))
        .expect(cache("pebble.tagCache", null))
        .expect(tagCache(0))
        .expect(locale(locale))
        .expect(build)
        .expect(bindEngine)
        .expect(renderer)
        .run(unit -> {
          new Pebble().doWith(b -> {
            assertNotNull(b);
            latch.countDown();
          })
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
    latch.await();
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void prod() throws Exception {
    Locale locale = Locale.getDefault();
    new MockUnit(Env.class, Config.class, Binder.class, PebbleEngine.class)
        .expect(defLoader)
        .expect(newEngine)
        .expect(env("prod", locale))
        .expect(cache("pebble.cache", "maximumSize=200"))
        .expect(cacheStatic)
        .expect(cacheBuilder(200))
        .expect(cache("pebble.tagCache", "maximumSize=100"))
        .expect(unit -> {
          Cache<BaseTagCacheKey, Object> cache = unit.mock(Cache.class);

          CacheBuilder cachebuilder = unit.mock(CacheBuilder.class);
          expect(CacheBuilder.from("maximumSize=100")).andReturn(cachebuilder);
          expect(cachebuilder.build()).andReturn(cache);

          PebbleEngine.Builder pebble = unit.get(PebbleEngine.Builder.class);
          expect(pebble.tagCache(cache)).andReturn(pebble);
        })
        .expect(locale(locale))
        .expect(build)
        .expect(bindEngine)
        .expect(renderer)
        .run(unit -> {
          new Pebble()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  private Block cacheBuilder(final int maxSize) {
    return unit -> {
      Cache<Object, PebbleTemplate> cache = unit.mock(Cache.class);
      unit.registerMock(Cache.class, cache);

      CacheBuilder cachebuilder = unit.mock(CacheBuilder.class);
      expect(CacheBuilder.from("maximumSize=" + maxSize)).andReturn(cachebuilder);
      expect(cachebuilder.build()).andReturn(cache);

      PebbleEngine.Builder pebble = unit.get(PebbleEngine.Builder.class);
      expect(pebble.templateCache(cache)).andReturn(pebble);
    };
  }

  private Block locale(final Locale locale) {
    return unit -> {
      PebbleEngine.Builder pebble = unit.get(PebbleEngine.Builder.class);
      expect(pebble.defaultLocale(locale)).andReturn(pebble);
    };
  }

  private Block cache(final String path, final String value) {
    return unit -> {
      Config conf = unit.get(Config.class);
      boolean has = value != null;
      expect(conf.hasPath(path)).andReturn(has);
      if (has) {
        expect(conf.getString(path)).andReturn(value);
      }
    };
  }

  private Block env(final String name, final Locale locale) {
    return unit -> {
      Env env = unit.get(Env.class);
      expect(env.name()).andReturn(name);
      expect(env.locale()).andReturn(locale);
    };
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  private Block cache(final int size) {
    return unit -> {
      Cache<Object, PebbleTemplate> cache = unit.mock(Cache.class);

      CacheBuilder cachebuilder = unit.mock(CacheBuilder.class);
      expect(CacheBuilder.newBuilder()).andReturn(cachebuilder);
      expect(cachebuilder.maximumSize(size)).andReturn(cachebuilder);
      expect(cachebuilder.build()).andReturn(cache);

      PebbleEngine.Builder pebble = unit.get(PebbleEngine.Builder.class);
      expect(pebble.templateCache(cache)).andReturn(pebble);
    };
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  private Block tagCache(final int size) {
    return unit -> {
      Cache<BaseTagCacheKey, Object> cache = unit.mock(Cache.class);

      CacheBuilder cachebuilder = unit.mock(CacheBuilder.class);
      expect(CacheBuilder.newBuilder()).andReturn(cachebuilder);
      expect(cachebuilder.maximumSize(size)).andReturn(cachebuilder);
      expect(cachebuilder.build()).andReturn(cache);

      PebbleEngine.Builder pebble = unit.get(PebbleEngine.Builder.class);
      expect(pebble.tagCache(cache)).andReturn(pebble);
    };
  }
}
