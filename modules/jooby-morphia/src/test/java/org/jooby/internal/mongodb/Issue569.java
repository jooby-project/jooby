package org.jooby.internal.mongodb;

import static org.easymock.EasyMock.expect;

import java.lang.reflect.Field;

import org.jooby.mongodb.GeneratedValue;
import org.jooby.mongodb.IdGen;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.mapping.MappedClass;
import org.mongodb.morphia.mapping.Mapper;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.mongodb.DBObject;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AutoIncID.class, Field.class, StoredId.class })
public class Issue569 {

  public static class MonDoc {
    @Id
    public Long id;
  }
  @Test
  public void shouldSkipNullID() throws Exception {
    Object entity = new Object();
    new MockUnit(Datastore.class, DBObject.class, Mapper.class, MappedClass.class, Field.class,
        GeneratedValue.class)
            .expect(unit -> {

              MappedClass mclass = unit.get(MappedClass.class);
              expect(mclass.getIdField()).andReturn(null);
              Mapper mapper = unit.get(Mapper.class);
              expect(mapper.getMappedClass(entity)).andReturn(mclass);
            })
            .run(unit -> {
              new AutoIncID(unit.get(Datastore.class), IdGen.LOCAL)
                  .prePersist(entity, unit.get(DBObject.class), unit.get(Mapper.class));
            });
  }

}
