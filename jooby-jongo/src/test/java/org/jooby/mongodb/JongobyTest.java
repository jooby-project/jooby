package org.jooby.mongodb;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

import org.jongo.Jongo;
import org.jongo.Mapper;
import org.jooby.Env;
import org.jooby.internal.mongodb.JongoFactoryImpl;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.typesafe.config.Config;

public class JongobyTest {

  @SuppressWarnings("unchecked")
  private Block mapper = unit -> {
    AnnotatedBindingBuilder<Mapper> abbM = unit.mock(AnnotatedBindingBuilder.class);
    abbM.toInstance(isA(Mapper.class));

    Binder binder = unit.get(Binder.class);
    expect(binder.bind(Mapper.class)).andReturn(abbM);
  };

  @SuppressWarnings("unchecked")
  private Block jongo = unit -> {
    AnnotatedBindingBuilder<Jongo> abbj = unit.mock(AnnotatedBindingBuilder.class);
    expect(abbj.toProvider(JongoFactoryImpl.class)).andReturn(null);

    Binder binder = unit.get(Binder.class);
    expect(binder.bind(Jongo.class)).andReturn(abbj);
  };

  @SuppressWarnings("unchecked")
  private Block jongoFactory = unit -> {
    AnnotatedBindingBuilder<JongoFactory> abbj = unit.mock(AnnotatedBindingBuilder.class);
    expect(abbj.to(JongoFactoryImpl.class)).andReturn(null);

    Binder binder = unit.get(Binder.class);
    expect(binder.bind(JongoFactory.class)).andReturn(abbj);
  };

  @Test
  public void defaults() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(mapper)
        .expect(jongo)
        .expect(jongoFactory)
        .run(unit -> {
          new Jongoby()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }
}
