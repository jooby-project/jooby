package org.jooby.banner;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

import javax.inject.Provider;

import org.jooby.Env;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.lalyos.jfiglet.FigletFont;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.name.Names;
import com.typesafe.config.Config;

import javaslang.control.Try.CheckedRunnable;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Banner.class, LoggerFactory.class, FigletFont.class })
public class BannerTest {

  private Block onStart = unit -> {
    Env env = unit.get(Env.class);

    expect(env.onStart(unit.capture(CheckedRunnable.class))).andReturn(env);
  };

  @Test
  public void configure() throws Exception {
    String banner = "banner";
    new MockUnit(Env.class, Config.class, Binder.class, Logger.class)
        .expect(conf("app", "1.0.0"))
        .expect(log("app"))
        .expect(banner())
        .expect(onStart)
        .run(unit -> {
          new Banner(banner)
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void trimEnd() throws Exception {
    String banner = "banner";
    new MockUnit(Env.class, Config.class, Binder.class, Logger.class)
        .expect(conf("app", "1.0.0"))
        .expect(log("app"))
        .expect(banner())
        .expect(onStart)
        .run(unit -> {
          new Banner(banner + "   ")
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void print() throws Exception {
    String banner = "banner";
    new MockUnit(Env.class, Config.class, Binder.class, Logger.class)
        .expect(conf("app", "1.0.0"))
        .expect(log("app"))
        .expect(onStart)
        .expect(convertOnLine(banner, "speed"))
        .expect(print(banner, "1.0.0"))
        .expect(banner())
        .run(unit -> {
          new Banner(banner)
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, unit -> {
          unit.captured(CheckedRunnable.class).iterator().next().run();
        });
  }

  @Test
  public void font() throws Exception {
    String banner = "banner";
    new MockUnit(Env.class, Config.class, Binder.class, Logger.class)
        .expect(conf("app", "1.0.0"))
        .expect(log("app"))
        .expect(onStart)
        .expect(convertOnLine(banner, "myfont"))
        .expect(print(banner, "1.0.0"))
        .expect(banner())
        .run(unit -> {
          new Banner(banner)
              .font("myfont")
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, unit -> {
          unit.captured(CheckedRunnable.class).iterator().next().run();
        });
  }

  @Test
  public void defprint() throws Exception {
    String banner = "app";
    new MockUnit(Env.class, Config.class, Binder.class, Logger.class)
        .expect(conf("app", "1.0.0"))
        .expect(log("app"))
        .expect(onStart)
        .expect(convertOnLine(banner, "speed"))
        .expect(print(banner, "1.0.0"))
        .expect(banner())
        .run(unit -> {
          new Banner()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, unit -> {
          unit.captured(CheckedRunnable.class).iterator().next().run();
        });
  }

  private Block print(final String banner, final String version) {
    return unit -> {
      Logger log = unit.get(Logger.class);
      log.info("\n{} v{}\n", banner, version);
    };
  }

  private Block convertOnLine(final String app, final String font) {
    return unit -> {
      unit.mockStatic(FigletFont.class);
      expect(FigletFont.convertOneLine("classpath:/flf/" + font + ".flf", app)).andReturn(app);
    };
  }

  private Block log(final String name) {
    return unit -> {
      unit.mockStatic(LoggerFactory.class);
      expect(LoggerFactory.getLogger(name)).andReturn(unit.get(Logger.class));
    };
  }

  private Block conf(final String name, final String v) {
    return unit -> {
      Config conf = unit.get(Config.class);
      expect(conf.getString("application.name")).andReturn(name);
      expect(conf.getString("application.version")).andReturn(v);
    };
  }

  @SuppressWarnings("unchecked")
  private Block banner() {
    return unit -> {

      LinkedBindingBuilder<String> lbb = unit.mock(LinkedBindingBuilder.class);
      expect(lbb.toProvider(isA(Provider.class))).andReturn(lbb);

      Binder binder = unit.get(Binder.class);
      expect(binder.bind(Key.get(String.class, Names.named("application.banner")))).andReturn(lbb);
    };
  }
}
