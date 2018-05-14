package org.jooby.guava;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import javax.inject.Inject;

import org.jooby.Env;
import org.jooby.test.MockUnit;
import org.junit.Test;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.ConfigurationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class GuavaCacheTest {

  public static class RequireCache {
    @Inject
    public RequireCache(final Cache<String, Object> cache) {
      requireNonNull(cache, "The cache is required.");
    }
  }

  public static class RequireLoadingCache {
    @Inject
    public RequireLoadingCache(final LoadingCache<String, Object> cache) {
      requireNonNull(cache, "The cache is required.");
    }
  }

  public static class RequireTypeSafeCache {
    @Inject
    public RequireTypeSafeCache(final Cache<Integer, String> cache) {
      requireNonNull(cache, "The cache is required.");
    }
  }

  @Test
  public void defaults() throws Exception {
    Config conf = ConfigFactory.empty()
        .withValue("guava.cache", ConfigValueFactory.fromAnyRef(""))
        .withValue("guava.session", ConfigValueFactory.fromAnyRef(""));
    new MockUnit(Env.class)
        .run(unit -> {
          Injector injector = Guice.createInjector(binder -> {
            GuavaCache.newCache().configure(unit.get(Env.class), conf, binder);
          });
          injector.getInstance(RequireCache.class);
          injector.getInstance(GuavaSessionStore.class);
        });
  }

  @Test
  public void loadingCache() throws Exception {
    Config conf = ConfigFactory.empty()
        .withValue("guava.cache", ConfigValueFactory.fromAnyRef(""))
        .withValue("guava.session", ConfigValueFactory.fromAnyRef(""));
    new MockUnit(Env.class)
        .run(unit -> {
          Injector injector = Guice.createInjector(binder -> {
            new GuavaCache<String, Object>(){}.doWith((n, b) ->{
              return b.build(new CacheLoader<String, Object>() {

                @Override
                public Object load(final String key) throws Exception {
                  return null;
                }

              });
            }).configure(unit.get(Env.class), conf, binder);
          });
          injector.getInstance(RequireCache.class);
          injector.getInstance(RequireLoadingCache.class);
          injector.getInstance(GuavaSessionStore.class);
        });
  }

  @SuppressWarnings("rawtypes")
  @Test
  public void defaultsNoSpec() throws Exception {
    Config conf = ConfigFactory.empty();
    new MockUnit(Env.class)
        .run(unit -> {
          Injector injector = Guice.createInjector(binder -> {
            new GuavaCache() {
            }.configure(unit.get(Env.class), conf, binder);
          });
          injector.getInstance(RequireCache.class);
        });
  }

  @SuppressWarnings("rawtypes")
  @Test
  public void oneCache() throws Exception {
    Config conf = ConfigFactory.empty()
        .withValue("guava.cache", ConfigValueFactory.fromAnyRef(""));
    new MockUnit(Env.class)
        .run(unit -> {
          Injector injector = Guice.createInjector(binder -> {
            new GuavaCache() {
            }.configure(unit.get(Env.class), conf, binder);
          });
          injector.getInstance(RequireCache.class);

          try {
            injector.getInstance(GuavaSessionStore.class);
            fail("No session found");
          } catch (ConfigurationException ex) {
            // OK
          }
        });
  }

  @SuppressWarnings("rawtypes")
  @Test
  public void cacheWithObjectSpec() throws Exception {
    Config conf = ConfigFactory.empty()
        .withValue("guava.cache.maximumSize", ConfigValueFactory.fromAnyRef(10))
        .withValue("guava.cache.concurrencyLevel", ConfigValueFactory.fromAnyRef(2));
    new MockUnit(Env.class)
        .run(unit -> {
          Injector injector = Guice.createInjector(binder -> {
            new GuavaCache() {
            }.configure(unit.get(Env.class), conf, binder);
          });
          injector.getInstance(RequireCache.class);

          try {
            injector.getInstance(GuavaSessionStore.class);
            fail("No session found");
          } catch (ConfigurationException ex) {
            // OK
          }
        });
  }

  @Test
  public void typeSafeCache() throws Exception {
    Config conf = ConfigFactory.empty()
        .withValue("guava.cache", ConfigValueFactory.fromAnyRef(""))
        .withValue("guava.session", ConfigValueFactory.fromAnyRef(""));
    new MockUnit(Env.class)
        .run(unit -> {
          Injector injector = Guice.createInjector(binder -> {
            new GuavaCache<Integer, String>() {
            }
                .configure(unit.get(Env.class), conf, binder);
          });
          injector.getInstance(RequireTypeSafeCache.class);
          // raw cache
          injector.getInstance(Cache.class);
          // raw cache for session
          injector.getInstance(Key.get(Cache.class, Names.named("session")));
        });
  }

  @Test
  public void withCallback() throws Exception {
    Config conf = ConfigFactory.empty()
        .withValue("guava.cache", ConfigValueFactory.fromAnyRef("maximumSize=10"));
    new MockUnit(Env.class)
        .run(unit -> {
          Injector injector = Guice.createInjector(binder -> {
            new GuavaCache<Integer, String>() {
            }.doWith((n, builder) -> {
              assertEquals("cache", n);
              assertNotNull(builder);
              return builder.build();
            })
                .configure(unit.get(Env.class), conf, binder);
          });
          injector.getInstance(RequireTypeSafeCache.class);
        });
  }

}
