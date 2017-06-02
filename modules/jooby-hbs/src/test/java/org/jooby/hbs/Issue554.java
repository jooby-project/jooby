package org.jooby.hbs;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

import org.jooby.Env;
import org.jooby.Renderer;
import org.jooby.internal.hbs.HbsEngine;
import org.jooby.internal.hbs.HbsHelpers;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.github.jknack.handlebars.Handlebars;
import com.google.inject.Binder;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.typesafe.config.Config;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Hbs.class, Multibinder.class })
public class Issue554 {

  @SuppressWarnings("unchecked")
  @Test
  public void doWith() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(unit -> {
          Env env = unit.get(Env.class);
          expect(env.name()).andReturn("dev");
        })
        .expect(unit -> {
          AnnotatedBindingBuilder<Handlebars> hABB = unit.mock(AnnotatedBindingBuilder.class);
          hABB.toInstance(isA(Handlebars.class));

          Binder binder = unit.get(Binder.class);
          expect(binder.bind(Handlebars.class)).andReturn(hABB);
        })
        .expect(unit -> {
          Binder binder = unit.get(Binder.class);

          Multibinder<Object> mbinder = unit.mock(Multibinder.class);

          unit.mockStatic(Multibinder.class);
          expect(Multibinder.newSetBinder(binder, Object.class, Names.named("hbs.helpers")))
              .andReturn(mbinder);

          LinkedBindingBuilder<Renderer> fLBB = unit.mock(LinkedBindingBuilder.class);
          fLBB.toInstance(isA(HbsEngine.class));

          Multibinder<Renderer> mfbinder = unit.mock(Multibinder.class);
          expect(mfbinder.addBinding()).andReturn(fLBB);
          expect(Multibinder.newSetBinder(binder, Renderer.class))
              .andReturn(mfbinder);

          AnnotatedBindingBuilder<HbsHelpers> hhABB = unit.mock(AnnotatedBindingBuilder.class);
          hhABB.asEagerSingleton();

          expect(binder.bind(HbsHelpers.class)).andReturn(hhABB);
        })
        .expect(unit -> {

        })
        .run(unit -> {
          new Hbs()
              .doWith(hbs -> {
                hbs.setStartDelimiter("<<");
                hbs.setEndDelimiter("<<");
              })
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }
}
