package org.jooby.internal.hotswap;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.UUID;

import org.jooby.Jooby;
import org.jooby.MockUnit;
import org.jooby.internal.AppManager;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.typesafe.config.Config;

public class HotswapTest {

  public static class App1 extends Jooby {
    {
      get("/", () -> "x");
    }
  }

  @Test
  public void dontReloadWhenNoRelevantChangeHasBeenMade() throws Exception {
    new MockUnit(Injector.class, AppManager.class, Config.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getString("application.builddir")).andReturn("target/classes");
          expect(config.getStringList("hotswap.reload.ext"))
              .andReturn(Lists.newArrayList("class", "conf", "properties"));
        })
        .expect(unit -> {
          Injector injector = unit.get(Injector.class);

          expect(injector.getInstance(Key.get(String.class, Names.named("internal.appClass"))))
              .andReturn(App1.class.getName());

          expect(injector.getInstance(AppManager.class)).andReturn(unit.get(AppManager.class));

          expect(injector.getInstance(Config.class)).andReturn(unit.get(Config.class));
        })
        .run(unit -> {

          Hotswap hotswap = new Hotswap(unit.get(Injector.class));

          /**
           * Don't change on same class
           */
          Injector reloaded = hotswap.reload(App1.class.getName());
          assertEquals(unit.get(Injector.class), reloaded);

        });
  }

  @Test
  public void dontReloadWhenFileHasNoExt() throws Exception {
    new MockUnit(Injector.class, AppManager.class, Config.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getString("application.builddir")).andReturn("target/classes");
          expect(config.getStringList("hotswap.reload.ext"))
              .andReturn(Lists.newArrayList("class", "conf", "properties"));
        })
        .expect(unit -> {
          Injector injector = unit.get(Injector.class);

          expect(injector.getInstance(Key.get(String.class, Names.named("internal.appClass"))))
              .andReturn(App1.class.getName());

          expect(injector.getInstance(AppManager.class)).andReturn(unit.get(AppManager.class));

          expect(injector.getInstance(Config.class)).andReturn(unit.get(Config.class));
        })
        .run(unit -> {

          Hotswap hotswap = new Hotswap(unit.get(Injector.class));

          /**
           * Don't change on same class
           */
          Injector reloaded = hotswap.reload("org/jooby/internal/hotswap/HotswapTest$App1");
          assertEquals(unit.get(Injector.class), reloaded);

        });
  }

  @Test
  public void reloadOnHashChanges() throws Exception {
    Injector newInjector = createMock(Injector.class);

    new MockUnit(Injector.class, AppManager.class, Config.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getString("application.builddir")).andReturn("target/classes");

          expect(config.getStringList("hotswap.reload.ext"))
              .andReturn(Lists.newArrayList("class", "conf", "properties"));
        })
        .expect(unit -> {
          AppManager manager = unit.get(AppManager.class);
          expect(manager.execute(0)).andReturn(newInjector);
        })
        .expect(unit -> {
          Injector injector = unit.get(Injector.class);

          expect(injector.getInstance(Key.get(String.class, Names.named("internal.appClass"))))
              .andReturn(App1.class.getName());

          expect(injector.getInstance(AppManager.class)).andReturn(unit.get(AppManager.class));

          expect(injector.getInstance(Config.class)).andReturn(unit.get(Config.class));
        })
        .run(unit -> {

          Hotswap hotswap = new Hotswap(unit.get(Injector.class)) {
            @Override
            protected List<String> hash(final String classname) {
              return Lists.newArrayList(UUID.randomUUID().toString());
            }
          };

          Injector reloaded = hotswap.reload("org/jooby/internal/hotswap/HotswapTest$App1.class");
          assertEquals(newInjector, reloaded);

        });
  }

  @Test
  public void reloadOnAynthingElse() throws Exception {
    Injector newInjector = createMock(Injector.class);

    new MockUnit(Injector.class, AppManager.class, Config.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getString("application.builddir")).andReturn("target/classes");

          expect(config.getStringList("hotswap.reload.ext"))
              .andReturn(Lists.newArrayList("class", "conf", "properties"));
        })
        .expect(unit -> {
          AppManager manager = unit.get(AppManager.class);
          expect(manager.execute(0)).andReturn(newInjector);
        })
        .expect(unit -> {
          Injector injector = unit.get(Injector.class);

          expect(injector.getInstance(Key.get(String.class, Names.named("internal.appClass"))))
              .andReturn(App1.class.getName());

          expect(injector.getInstance(AppManager.class)).andReturn(unit.get(AppManager.class));

          expect(injector.getInstance(Config.class)).andReturn(unit.get(Config.class));
        })
        .run(unit -> {

          Hotswap hotswap = new Hotswap(unit.get(Injector.class));

          Injector reloaded = hotswap.reload("org/jooby/internal/hotswap/HotswapTest.class");
          assertEquals(newInjector, reloaded);

        });
  }

  @Test
  public void onChangeCallReload() throws Exception {
    Injector newInjector = createMock(Injector.class);

    new MockUnit(Injector.class, AppManager.class, Config.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getString("application.builddir")).andReturn("target/classes");

          expect(config.getStringList("hotswap.reload.ext"))
              .andReturn(Lists.newArrayList("class", "conf", "properties"));
        })
        .expect(unit -> {
          AppManager manager = unit.get(AppManager.class);
          expect(manager.execute(0)).andReturn(newInjector);
        })
        .expect(unit -> {
          Injector injector = unit.get(Injector.class);

          expect(injector.getInstance(Key.get(String.class, Names.named("internal.appClass"))))
              .andReturn(App1.class.getName());

          expect(injector.getInstance(AppManager.class)).andReturn(unit.get(AppManager.class));

          expect(injector.getInstance(Config.class)).andReturn(unit.get(Config.class));
        })
        .run(unit -> {

          Hotswap hotswap = new Hotswap(unit.get(Injector.class));

          hotswap.changed("org/jooby/internal/hotswap/HotswapTest.class");

        });
  }

  @Test
  public void stopOnErr() throws Exception {
    new MockUnit(Injector.class, AppManager.class, Config.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getString("application.builddir")).andReturn("target/classes");

          expect(config.getStringList("hotswap.reload.ext"))
              .andReturn(Lists.newArrayList("class", "conf", "properties"));
        })
        .expect(unit -> {
          AppManager manager = unit.get(AppManager.class);
          expect(manager.execute(0)).andThrow(new RuntimeException());

          expect(manager.execute(-1)).andReturn(unit.get(Injector.class));
        })
        .expect(unit -> {
          Injector injector = unit.get(Injector.class);

          expect(injector.getInstance(Key.get(String.class, Names.named("internal.appClass"))))
              .andReturn(App1.class.getName());

          expect(injector.getInstance(AppManager.class)).andReturn(unit.get(AppManager.class));

          expect(injector.getInstance(Config.class)).andReturn(unit.get(Config.class));
        })
        .run(unit -> {

          Hotswap hotswap = new Hotswap(unit.get(Injector.class));

          hotswap.reload("org/jooby/internal/hotswap/HotswapTest.class");

        });
  }

}
