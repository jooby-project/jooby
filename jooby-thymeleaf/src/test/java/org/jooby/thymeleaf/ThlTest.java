package org.jooby.thymeleaf;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.atomic.AtomicReference;

import org.jooby.Env;
import org.jooby.Renderer;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

import com.google.inject.Binder;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import com.typesafe.config.Config;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Thl.class, ClassLoaderTemplateResolver.class, TemplateEngine.class,
    ThlEngine.class, Multibinder.class })
public class ThlTest {

  @SuppressWarnings({"rawtypes", "unchecked" })
  private Block templateEngine = unit -> {
    TemplateEngine engine = unit.constructor(TemplateEngine.class)
        .build();
    engine.setTemplateResolver(unit.get(ITemplateResolver.class));

    AnnotatedBindingBuilder abb = unit.mock(AnnotatedBindingBuilder.class);
    abb.toInstance(engine);
    abb.toInstance(engine);

    Binder binder = unit.get(Binder.class);
    expect(binder.bind(TemplateEngine.class)).andReturn(abb);
    expect(binder.bind(ITemplateEngine.class)).andReturn(abb);

    unit.registerMock(TemplateEngine.class, engine);
  };

  @SuppressWarnings({"rawtypes", "unchecked" })
  private Block viewEngine = unit -> {
    ThlEngine vengine = unit.constructor(ThlEngine.class)
        .args(TemplateEngine.class, Env.class)
        .build(unit.get(TemplateEngine.class), unit.get(Env.class));

    unit.mockStatic(Multibinder.class);

    LinkedBindingBuilder lbb = unit.mock(LinkedBindingBuilder.class);
    lbb.toInstance(vengine);
    Multibinder mbinder = unit.mock(Multibinder.class);
    expect(mbinder.addBinding()).andReturn(lbb);
    expect(Multibinder.newSetBinder(unit.get(Binder.class), Renderer.class)).andReturn(mbinder);
  };

  @Test
  public void defaults() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(env("dev"))
        .expect(templateResolver("/", ".html", false))
        .expect(templateEngine)
        .expect(viewEngine)
        .run(unit -> {
          new Thl()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void doWith() throws Exception {
    AtomicReference<TemplateEngine> engine = new AtomicReference<>();
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(env("dev"))
        .expect(templateResolver("/", ".html", false))
        .expect(templateEngine)
        .expect(viewEngine)
        .run(unit -> {
          new Thl()
              .doWith(engine::set)
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
    assertNotNull(engine.get());
  }

  @Test
  public void shouldSetPrefixAndSuffix() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(env("dev"))
        .expect(templateResolver("/templates", ".thl.html", false))
        .expect(templateEngine)
        .expect(viewEngine)
        .run(unit -> {
          new Thl("/templates", ".thl.html")
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void shouldSetCacheable() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(env("prod"))
        .expect(templateResolver("/", ".html", true))
        .expect(templateEngine)
        .expect(viewEngine)
        .run(unit -> {
          new Thl()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  private Block env(final String env) {
    return unit -> {
      Env e = unit.get(Env.class);
      expect(e.name()).andReturn(env);
    };
  }

  private Block templateResolver(final String prefix, final String suffix,
      final boolean cacheable) {
    return unit -> {
      ClassLoaderTemplateResolver tr = unit.constructor(ClassLoaderTemplateResolver.class)
          .build();
      tr.setCacheable(cacheable);
      tr.setPrefix(prefix);
      tr.setSuffix(suffix);
      tr.setTemplateMode(TemplateMode.HTML);
      unit.registerMock(ITemplateResolver.class, tr);
    };
  }
}
