package org.jooby.ehcache;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

import java.util.Collections;
import java.util.Map;

import org.jooby.Env;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.inject.Binder;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import javaslang.control.Try.CheckedRunnable;
import net.sf.ehcache.CacheManager;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Eh.class })
public class EhCacheTest {

  @SuppressWarnings("unchecked")
  @Test
  public void defaults() throws Exception {

    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(unit -> {
          Config ehcache = unit.get(Config.class);

          expect(ehcache.getConfig("ehcache")).andReturn(empty());
        })
        .expect(unit -> {
          Binder binder = unit.get(Binder.class);

          AnnotatedBindingBuilder<CacheManager> abbCM = unit.mock(AnnotatedBindingBuilder.class);
          abbCM.toInstance(isA(CacheManager.class));

          expect(binder.bind(CacheManager.class)).andReturn(abbCM);
        })
        .expect(unit -> {
          Env env = unit.get(Env.class);
          expect(env.onStop(isA(CheckedRunnable.class))).andReturn(env);
        })
        .run(unit -> {
          new Eh()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  private Config empty() {
    return ConfigFactory.empty()
          .withValue("cache.default", ConfigValueFactory.fromAnyRef(defaultCache()));
  }

  private Map<String, Object> defaultCache() {
    return Collections.emptyMap();
  }
}
