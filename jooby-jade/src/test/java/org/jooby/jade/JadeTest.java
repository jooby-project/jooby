package org.jooby.jade;

import com.google.inject.Binder;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import com.typesafe.config.Config;
import de.neuland.jade4j.JadeConfiguration;
import de.neuland.jade4j.template.ClasspathTemplateLoader;
import org.jooby.Env;
import org.jooby.Renderer;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.expect;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Jade.class, JadeConfiguration.class, Multibinder.class})
public class JadeTest {

  @Test
  public void defaults() throws Exception {
    String suffix = ".jade";

    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(unit -> {
          Env env = unit.get(Env.class);
          expect(env.name()).andReturn("dev");

          Config config = unit.get(Config.class);
          expect(config.hasPath("jade.prettyprint")).andReturn(false);
          expect(config.hasPath("jade.suffix")).andReturn(false);
        })
        .expect(unit -> {
          JadeConfiguration jadeConfiguration = unit.mockConstructor(JadeConfiguration.class);
          jadeConfiguration.setCaching(false);
          jadeConfiguration.setPrettyPrint(false);

          Env env = unit.get(Env.class);
          Map<String, Object> sharedVariables = new HashMap<>(1);
          sharedVariables.put("env", env);
          jadeConfiguration.setSharedVariables(sharedVariables);

          ClasspathTemplateLoader classpathTemplateLoader = unit.mockConstructor(ClasspathTemplateLoader.class);
          jadeConfiguration.setTemplateLoader(classpathTemplateLoader);

          AnnotatedBindingBuilder<JadeConfiguration> configBB = unit.mock(AnnotatedBindingBuilder.class);
          configBB.toInstance(jadeConfiguration);

          Binder binder = unit.get(Binder.class);
          expect(binder.bind(JadeConfiguration.class)).andReturn(configBB);

          Engine engine = unit.mockConstructor(
              Engine.class, new Class[]{JadeConfiguration.class, String.class},
              jadeConfiguration, suffix);

          LinkedBindingBuilder<Renderer> ffLBB = unit.mock(LinkedBindingBuilder.class);
          ffLBB.toInstance(engine);

          Multibinder<Renderer> formatter = unit.mock(Multibinder.class);
          expect(formatter.addBinding()).andReturn(ffLBB);

          unit.mockStatic(Multibinder.class);
          expect(Multibinder.newSetBinder(binder, Renderer.class)).andReturn(formatter);
        })
        .run(unit -> {
          new Jade()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void testConfigurableSuffix() throws Exception {
    String suffix = ".html";
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(unit -> {
          Env env = unit.get(Env.class);
          expect(env.name()).andReturn("dev");

          Config config = unit.get(Config.class);
          expect(config.hasPath("jade.prettyprint")).andReturn(false);
          expect(config.hasPath("jade.suffix")).andReturn(true);
          expect(config.getString("jade.suffix")).andReturn(suffix);
        })
        .expect(unit -> {
          JadeConfiguration jadeConfiguration = unit.mockConstructor(JadeConfiguration.class);
          jadeConfiguration.setCaching(false);
          jadeConfiguration.setPrettyPrint(false);

          Env env = unit.get(Env.class);
          Map<String, Object> sharedVariables = new HashMap<>(1);
          sharedVariables.put("env", env);
          jadeConfiguration.setSharedVariables(sharedVariables);

          ClasspathTemplateLoader classpathTemplateLoader = unit.mockConstructor(ClasspathTemplateLoader.class);
          jadeConfiguration.setTemplateLoader(classpathTemplateLoader);

          AnnotatedBindingBuilder<JadeConfiguration> configBB = unit.mock(AnnotatedBindingBuilder.class);
          configBB.toInstance(jadeConfiguration);

          Binder binder = unit.get(Binder.class);
          expect(binder.bind(JadeConfiguration.class)).andReturn(configBB);

          Engine engine = unit.mockConstructor(
              Engine.class, new Class[]{JadeConfiguration.class, String.class},
              jadeConfiguration, suffix);

          LinkedBindingBuilder<Renderer> ffLBB = unit.mock(LinkedBindingBuilder.class);
          ffLBB.toInstance(engine);

          Multibinder<Renderer> formatter = unit.mock(Multibinder.class);
          expect(formatter.addBinding()).andReturn(ffLBB);

          unit.mockStatic(Multibinder.class);
          expect(Multibinder.newSetBinder(binder, Renderer.class)).andReturn(formatter);
        })
        .run(unit -> {
          new Jade()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void testCachingDisabledInProduction() throws Exception {
    String suffix = ".jade";

    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(unit -> {
          Env env = unit.get(Env.class);
          expect(env.name()).andReturn("prod");

          Config config = unit.get(Config.class);
          expect(config.hasPath("jade.prettyprint")).andReturn(false);
          expect(config.hasPath("jade.suffix")).andReturn(false);
        })
        .expect(unit -> {
          JadeConfiguration jadeConfiguration = unit.mockConstructor(JadeConfiguration.class);
          jadeConfiguration.setCaching(true);
          jadeConfiguration.setPrettyPrint(false);

          Env env = unit.get(Env.class);
          Map<String, Object> sharedVariables = new HashMap<>(1);
          sharedVariables.put("env", env);
          jadeConfiguration.setSharedVariables(sharedVariables);

          ClasspathTemplateLoader classpathTemplateLoader = unit.mockConstructor(ClasspathTemplateLoader.class);
          jadeConfiguration.setTemplateLoader(classpathTemplateLoader);

          AnnotatedBindingBuilder<JadeConfiguration> configBB = unit.mock(AnnotatedBindingBuilder.class);
          configBB.toInstance(jadeConfiguration);

          Binder binder = unit.get(Binder.class);
          expect(binder.bind(JadeConfiguration.class)).andReturn(configBB);

          Engine engine = unit.mockConstructor(
              Engine.class, new Class[]{JadeConfiguration.class, String.class},
              jadeConfiguration, suffix);

          LinkedBindingBuilder<Renderer> ffLBB = unit.mock(LinkedBindingBuilder.class);
          ffLBB.toInstance(engine);

          Multibinder<Renderer> formatter = unit.mock(Multibinder.class);
          expect(formatter.addBinding()).andReturn(ffLBB);

          unit.mockStatic(Multibinder.class);
          expect(Multibinder.newSetBinder(binder, Renderer.class)).andReturn(formatter);
        })
        .run(unit -> {
          new Jade()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void testPrettyPrinting() throws Exception {
    String suffix = ".jade";

    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(unit -> {
          Env env = unit.get(Env.class);
          expect(env.name()).andReturn("dev");

          Config config = unit.get(Config.class);
          expect(config.hasPath("jade.prettyprint")).andReturn(true);
          expect(config.getBoolean("jade.prettyprint")).andReturn(true);
          expect(config.hasPath("jade.suffix")).andReturn(false);
        })
        .expect(unit -> {
          JadeConfiguration jadeConfiguration = unit.mockConstructor(JadeConfiguration.class);
          jadeConfiguration.setCaching(false);
          jadeConfiguration.setPrettyPrint(true);

          Env env = unit.get(Env.class);
          Map<String, Object> sharedVariables = new HashMap<>(1);
          sharedVariables.put("env", env);
          jadeConfiguration.setSharedVariables(sharedVariables);

          ClasspathTemplateLoader classpathTemplateLoader = unit.mockConstructor(ClasspathTemplateLoader.class);
          jadeConfiguration.setTemplateLoader(classpathTemplateLoader);

          AnnotatedBindingBuilder<JadeConfiguration> configBB = unit.mock(AnnotatedBindingBuilder.class);
          configBB.toInstance(jadeConfiguration);

          Binder binder = unit.get(Binder.class);
          expect(binder.bind(JadeConfiguration.class)).andReturn(configBB);

          Engine engine = unit.mockConstructor(
              Engine.class, new Class[]{JadeConfiguration.class, String.class},
              jadeConfiguration, suffix);

          LinkedBindingBuilder<Renderer> ffLBB = unit.mock(LinkedBindingBuilder.class);
          ffLBB.toInstance(engine);

          Multibinder<Renderer> formatter = unit.mock(Multibinder.class);
          expect(formatter.addBinding()).andReturn(ffLBB);

          unit.mockStatic(Multibinder.class);
          expect(Multibinder.newSetBinder(binder, Renderer.class)).andReturn(formatter);
        })
        .run(unit -> {
          new Jade()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

}
