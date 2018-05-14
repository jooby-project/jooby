package org.jooby.jade;

import com.google.inject.Binder;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import com.typesafe.config.Config;
import de.neuland.jade4j.JadeConfiguration;
import de.neuland.jade4j.template.ClasspathTemplateLoader;
import de.neuland.jade4j.template.TemplateLoader;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import org.jooby.Env;
import org.jooby.Renderer;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.FileNotFoundException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Jade.class, JadeConfiguration.class, Multibinder.class, HashMap.class })
public class JadeTest {

  @Test
  public void defaults() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(env("dev"))
        .expect(conf(".jade", false, false))
        .expect(jade("jade", false, true))
        .run(unit -> {
          new Jade()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void testConfigurableSuffix() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(env("dev"))
        .expect(conf(".html", false, false))
        .expect(jade("html", false, true))
        .run(unit -> {
          new Jade(".html")
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void testCachingOn() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(env("prod"))
        .expect(conf(".jade", true, false))
        .expect(jade("jade", true, false))
        .run(unit -> {
          new Jade()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void testCachingOnInProduction() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(env("prod"))
        .expect(conf(".jade", false, false))
        .expect(jade("jade", true, false))
        .run(unit -> {
          new Jade()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void testPrettyPrinting() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(env("dev"))
        .expect(conf(".jade", false, true))
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getBoolean("jade.prettyprint")).andReturn(true);
        })
        .expect(jade("jade", false, true))
        .run(unit -> {
          new Jade()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void doWith() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(env("dev"))
        .expect(conf(".jade", false, false))
        .expect(jade("jade", false, true))
        .run(unit -> {
          new Jade()
              .doWith(jade -> assertNotNull(jade))
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void safeTemplateLoader() throws Exception {
    new MockUnit(TemplateLoader.class, Reader.class)
        .expect(unit -> {
          TemplateLoader tl = unit.get(TemplateLoader.class);
          expect(tl.getLastModified("x")).andReturn(678L);
          expect(tl.getReader("foo")).andReturn(unit.get(Reader.class));
          expect(tl.getReader("fnf")).andThrow(new NullPointerException());
        })
        .run(unit -> {
          TemplateLoader tl = new Jade.IOTemplateLoader(unit.get(TemplateLoader.class));
          assertEquals(678L, tl.getLastModified("x"));
          assertEquals(unit.get(Reader.class), tl.getReader("foo"));

          try {
            assertEquals(unit.get(Reader.class), tl.getReader("fnf"));
            fail("expecting fnf");
          } catch (FileNotFoundException x) {

          }
        });
  }

  @SuppressWarnings("unchecked")
  private Block jade(final String suffix, final boolean caching, final boolean prettyPrint) {
    return unit -> {
      JadeConfiguration jadeConfiguration = unit.mockConstructor(JadeConfiguration.class);
      jadeConfiguration.setCaching(caching);
      jadeConfiguration.setPrettyPrint(prettyPrint);

      Env env = unit.get(Env.class);
      Map<String, Object> sharedVariables = unit.constructor(HashMap.class)
          .build(2);
      expect(sharedVariables.put(eq( "env"), eq(env))).andReturn(null);
      expect(sharedVariables.put(eq( "xss"), isA(XssHelper.class))).andReturn(null);
      jadeConfiguration.setSharedVariables(sharedVariables);

      ClasspathTemplateLoader classpathTemplateLoader = unit
          .constructor(ClasspathTemplateLoader.class)
          .build("UTF-8", suffix);
      jadeConfiguration.setTemplateLoader(classpathTemplateLoader);
      expect(jadeConfiguration.getTemplateLoader()).andReturn(classpathTemplateLoader);

      Jade.IOTemplateLoader safetl = unit
          .constructor(Jade.IOTemplateLoader.class)
          .build(classpathTemplateLoader);
      jadeConfiguration.setTemplateLoader(safetl);

      AnnotatedBindingBuilder<JadeConfiguration> configBB = unit
          .mock(AnnotatedBindingBuilder.class);
      configBB.toInstance(jadeConfiguration);

      Binder binder = unit.get(Binder.class);
      expect(binder.bind(JadeConfiguration.class)).andReturn(configBB);

      Engine engine = unit.mockConstructor(
          Engine.class, new Class[]{JadeConfiguration.class, String.class },
          jadeConfiguration, "." + suffix);

      LinkedBindingBuilder<Renderer> ffLBB = unit.mock(LinkedBindingBuilder.class);
      ffLBB.toInstance(engine);

      Multibinder<Renderer> formatter = unit.mock(Multibinder.class);
      expect(formatter.addBinding()).andReturn(ffLBB);

      unit.mockStatic(Multibinder.class);
      expect(Multibinder.newSetBinder(binder, Renderer.class)).andReturn(formatter);
    };
  }

  private Block env(final String name) {
    return unit -> {
      Env env = unit.get(Env.class);
      expect(env.name()).andReturn(name);
    };
  }

  private Block conf(final String suffix, final boolean caching, final boolean prettyprint) {
    return unit -> {
      Config config = unit.get(Config.class);
      expect(config.hasPath("jade.caching")).andReturn(caching);
      if (caching) {
        expect(config.getBoolean("jade.caching")).andReturn(caching);
      }
      expect(config.hasPath("jade.prettyprint")).andReturn(prettyprint);
    };
  }

}
