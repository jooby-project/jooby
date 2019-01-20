package org.jooby.json;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.jooby.Env;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.jooby.Parser;
import org.jooby.Renderer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Binder;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import com.typesafe.config.Config;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Gzon.class, Gson.class, GsonBuilder.class, Multibinder.class })
public class GzonTest {

  @SuppressWarnings("unchecked")
  private Block body = unit -> {
    GsonBuilder gsonb = unit.mockConstructor(GsonBuilder.class);
    unit.registerMock(GsonBuilder.class, gsonb);

    Gson gson = unit.get(Gson.class);

    AnnotatedBindingBuilder<Gson> abbGson = unit.mock(AnnotatedBindingBuilder.class);
    abbGson.toInstance(gson);

    Binder binder = unit.get(Binder.class);
    expect(binder.bind(Gson.class)).andReturn(abbGson);

    expect(gsonb.create()).andReturn(gson);

    LinkedBindingBuilder<Parser> lbbparser = unit.mock(LinkedBindingBuilder.class);
    lbbparser.toInstance(isA(GsonParser.class));

    Multibinder<Parser> mbparser = unit.mock(Multibinder.class);
    expect(mbparser.addBinding()).andReturn(lbbparser);

    LinkedBindingBuilder<Renderer> lbbrenderer = unit.mock(LinkedBindingBuilder.class);
    lbbrenderer.toInstance(isA(GsonRenderer.class));

    Multibinder<Renderer> mbrenderer = unit.mock(Multibinder.class);
    expect(mbrenderer.addBinding()).andReturn(lbbrenderer);

    unit.mockStatic(Multibinder.class);
    expect(Multibinder.newSetBinder(binder, Parser.class)).andReturn(mbparser);
    expect(Multibinder.newSetBinder(binder, Renderer.class)).andReturn(mbrenderer);
  };

  @Test
  public void defaults() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, Gson.class)
        .expect(body)
        .run(unit -> {
          new Gzon()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void raw() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, Gson.class)
        .expect(body)
        .run(unit -> {
          new Gzon()
              .raw()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void withCallback() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, Gson.class, Consumer.class)
        .expect(body)
        .expect(unit -> {
          unit.get(Consumer.class).accept(unit.get(GsonBuilder.class));
        })
        .run(unit -> {
          new Gzon()
              .doWith(unit.get(Consumer.class))
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void with2ArgsCallback() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, Gson.class, BiConsumer.class)
        .expect(body)
        .expect(unit -> {
          unit.get(BiConsumer.class).accept(unit.get(GsonBuilder.class), unit.get(Config.class));
        })
        .run(unit -> {
          new Gzon()
              .doWith(unit.get(BiConsumer.class))
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

}
