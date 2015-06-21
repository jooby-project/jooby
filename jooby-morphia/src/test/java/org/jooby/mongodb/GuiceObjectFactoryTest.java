package org.jooby.mongodb;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.jooby.test.MockUnit;
import org.junit.Test;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.ObjectFactory;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.mapping.MapperOptions;

import com.google.inject.Injector;
import com.mongodb.DBObject;

public class GuiceObjectFactoryTest {

  public static class Injectable {

    @Inject
    public Injectable() {
    }
  }

  MockUnit.Block boot = unit -> {
    ObjectFactory delegate = unit.get(ObjectFactory.class);

    MapperOptions options = unit.mock(MapperOptions.class);
    expect(options.getObjectFactory()).andReturn(delegate);
    options.setObjectFactory(isA(GuiceObjectFactory.class));

    Mapper mapper = unit.get(Mapper.class);
    expect(mapper.getOptions()).andReturn(options);

    Morphia morphia = unit.get(Morphia.class);
    expect(morphia.getMapper()).andReturn(mapper);
  };

  @Test
  public void createInstanceFromClazz() throws Exception {
    Injectable injectable = new Injectable();

    new MockUnit(Injector.class, Morphia.class, Mapper.class, ObjectFactory.class)
        .expect(boot)
        .expect(unit -> {
          Injector injector = unit.get(Injector.class);
          expect(injector.getInstance(Injectable.class)).andReturn(injectable);
        })
        .run(unit -> {
          assertEquals(injectable,
              new GuiceObjectFactory(unit.get(Injector.class), unit.get(Morphia.class))
                  .createInstance(Injectable.class));

        });
  }

  @Test
  public void createInstanceFromClazzNoInjectable() throws Exception {
    GuiceObjectFactoryTest expected = new GuiceObjectFactoryTest();

    new MockUnit(Injector.class, Morphia.class, Mapper.class, ObjectFactory.class)
        .expect(boot)
        .expect(unit -> {
          ObjectFactory injector = unit.get(ObjectFactory.class);
          expect(injector.createInstance(GuiceObjectFactoryTest.class)).andReturn(expected);
        })
        .run(unit -> {
          assertEquals(expected,
              new GuiceObjectFactory(unit.get(Injector.class), unit.get(Morphia.class))
                  .createInstance(GuiceObjectFactoryTest.class));

        });
  }

  @Test
  public void createInstanceFromClazzWithDBObject() throws Exception {
    Injectable injectable = new Injectable();

    new MockUnit(Injector.class, Morphia.class, Mapper.class, ObjectFactory.class, DBObject.class)
        .expect(boot)
        .expect(unit -> {
          Injector injector = unit.get(Injector.class);
          expect(injector.getInstance(Injectable.class)).andReturn(injectable);
        })
        .run(unit -> {
          assertEquals(injectable,
              new GuiceObjectFactory(unit.get(Injector.class), unit.get(Morphia.class))
                  .createInstance(Injectable.class, unit.get(DBObject.class)));

        });
  }

  @Test
  public void createInstanceFromClazzWithDBObjectNoInjectable() throws Exception {
    GuiceObjectFactoryTest expected = new GuiceObjectFactoryTest();

    new MockUnit(Injector.class, Morphia.class, Mapper.class, ObjectFactory.class, DBObject.class)
        .expect(boot)
        .expect(unit -> {
          ObjectFactory factory = unit.get(ObjectFactory.class);
          expect(factory.createInstance(GuiceObjectFactoryTest.class, unit.get(DBObject.class)))
              .andReturn(expected);
        })
        .run(unit -> {
          assertEquals(expected,
              new GuiceObjectFactory(unit.get(Injector.class), unit.get(Morphia.class))
                  .createInstance(GuiceObjectFactoryTest.class, unit.get(DBObject.class)));

        });
  }

  @Test
  public void createInstanceFully() throws Exception {
    Injectable injectable = new Injectable();

    new MockUnit(Injector.class, Morphia.class, Mapper.class, ObjectFactory.class, DBObject.class,
        MappedField.class)
        .expect(boot)
        .expect(unit -> {
          MappedField mf = unit.get(MappedField.class);
          expect(mf.getType()).andReturn(Injectable.class);

          Injector injector = unit.get(Injector.class);
          expect(injector.getInstance(Injectable.class)).andReturn(injectable);
        })
        .run(
            unit -> {
              assertEquals(
                  injectable,
                  new GuiceObjectFactory(unit.get(Injector.class), unit.get(Morphia.class))
                      .createInstance(unit.get(Mapper.class), unit.get(MappedField.class),
                          unit.get(DBObject.class)));

            });
  }

  @Test
  public void createInstanceFullyNoInjectable() throws Exception {
    GuiceObjectFactoryTest expected = new GuiceObjectFactoryTest();

    new MockUnit(Injector.class, Morphia.class, Mapper.class, ObjectFactory.class, DBObject.class,
        MappedField.class)
        .expect(boot)
        .expect(unit -> {
          MappedField mf = unit.get(MappedField.class);
          expect(mf.getType()).andReturn(GuiceObjectFactoryTest.class);

          ObjectFactory factory = unit.get(ObjectFactory.class);
          expect(factory.createInstance(unit.get(Mapper.class), mf, unit.get(DBObject.class)))
              .andReturn(expected);
        })
        .run(unit -> {
          assertEquals(
              expected,
              new GuiceObjectFactory(unit.get(Injector.class), unit.get(Morphia.class))
                  .createInstance(unit.get(Mapper.class), unit.get(MappedField.class),
                      unit.get(DBObject.class)));

        });
  }

  @Test
  public void createMap() throws Exception {
    Map<String, Object> expected = Collections.emptyMap();

    new MockUnit(Injector.class, Morphia.class, Mapper.class, ObjectFactory.class,
        MappedField.class)
        .expect(boot)
        .expect(unit -> {
          ObjectFactory factory = unit.get(ObjectFactory.class);
          expect(factory.createMap(unit.get(MappedField.class))).andReturn(expected);
        })
        .run(unit -> {
          assertEquals(
              expected,
              new GuiceObjectFactory(unit.get(Injector.class), unit.get(Morphia.class))
                  .createMap(unit.get(MappedField.class)));

        });
  }

  @Test
  public void createSet() throws Exception {
    Set<String> expected = Collections.emptySet();

    new MockUnit(Injector.class, Morphia.class, Mapper.class, ObjectFactory.class,
        MappedField.class)
        .expect(boot)
        .expect(unit -> {
          ObjectFactory factory = unit.get(ObjectFactory.class);
          expect(factory.createSet(unit.get(MappedField.class))).andReturn(expected);
        })
        .run(unit -> {
          assertEquals(
              expected,
              new GuiceObjectFactory(unit.get(Injector.class), unit.get(Morphia.class))
                  .createSet(unit.get(MappedField.class)));

        });
  }

  @Test
  public void createList() throws Exception {
    List<String> expected = Collections.emptyList();

    new MockUnit(Injector.class, Morphia.class, Mapper.class, ObjectFactory.class,
        MappedField.class)
        .expect(boot)
        .expect(unit -> {
          ObjectFactory factory = unit.get(ObjectFactory.class);
          expect(factory.createList(unit.get(MappedField.class))).andReturn(expected);
        })
        .run(unit -> {
          assertEquals(
              expected,
              new GuiceObjectFactory(unit.get(Injector.class), unit.get(Morphia.class))
                  .createList(unit.get(MappedField.class)));

        });
  }

}
