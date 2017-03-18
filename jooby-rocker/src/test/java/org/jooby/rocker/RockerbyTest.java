package org.jooby.rocker;

import static org.easymock.EasyMock.expect;

import org.jooby.Env;
import org.jooby.Renderer;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.inject.Binder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import com.typesafe.config.Config;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Rockerby.class, RockerRenderer.class, Multibinder.class })
public class RockerbyTest {

  @SuppressWarnings({"rawtypes", "unchecked" })
  @Test
  public void newInstance() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(unit -> {
          RockerRenderer renderer = unit.constructor(RockerRenderer.class)
              .build("", ".rocker.html");
          Binder binder = unit.get(Binder.class);
          unit.mockStatic(Multibinder.class);
          Multibinder mb = unit.mock(Multibinder.class);
          LinkedBindingBuilder lbb = unit.mock(LinkedBindingBuilder.class);
          lbb.toInstance(renderer);
          expect(mb.addBinding()).andReturn(lbb);
          expect(Multibinder.newSetBinder(binder, Renderer.class)).andReturn(mb);
        })
        .run(unit -> {
          new Rockerby().configure(unit.get(Env.class), unit.get(Config.class),
              unit.get(Binder.class));
        });
  }
}
