package org.jooby.internal.mongodb;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

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
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.mongodb.DBObject;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AutoIncID.class, Field.class, StoredId.class })
public class AutoIncIDTest {

  public static class MonDoc {
    @Id
    public Long id;
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  @Test
  public void prePersitInit() throws Exception {
    Object entity = new Object();
    new MockUnit(Datastore.class, DBObject.class, Mapper.class, MappedClass.class, Field.class,
        GeneratedValue.class)
            .expect(unit -> {
              GeneratedValue genval = unit.get(GeneratedValue.class);

              Field idField = unit.get(Field.class);
              expect(idField.getAnnotation(GeneratedValue.class)).andReturn(genval);
            })
            .expect(unit -> {
              Field idField = unit.get(Field.class);
              idField.setAccessible(true);
              idField.set(entity, null);

              MappedClass mclass = unit.get(MappedClass.class);
              expect(mclass.getIdField()).andReturn(idField);
              Class clazz = MonDoc.class;
              expect(mclass.getClazz()).andReturn(clazz);

              Mapper mapper = unit.get(Mapper.class);
              expect(mapper.getMappedClass(entity)).andReturn(mclass);

              Query<StoredId> query = unit.mock(Query.class);

              UpdateOperations<StoredId> uops = unit.mock(UpdateOperations.class);
              expect(uops.inc("value")).andReturn(uops);

              StoredId storedId = unit.mockConstructor(StoredId.class, MonDoc.class.getName());

              Datastore ds = unit.get(Datastore.class);
              expect(ds.find(StoredId.class, "_id", MonDoc.class.getName())).andReturn(query);
              expect(ds.createUpdateOperations(StoredId.class)).andReturn(uops);
              expect(ds.findAndModify(query, uops)).andReturn(null);
              expect(ds.save(storedId)).andReturn(null);
            })
            .run(unit -> {
              new AutoIncID(unit.get(Datastore.class), IdGen.LOCAL)
                  .prePersist(entity, unit.get(DBObject.class), unit.get(Mapper.class));
            });
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  @Test
  public void prePersit() throws Exception {
    Object entity = new Object();
    new MockUnit(Datastore.class, DBObject.class, Mapper.class, MappedClass.class, Field.class,
        GeneratedValue.class)
            .expect(unit -> {
              GeneratedValue genval = unit.get(GeneratedValue.class);

              Field idField = unit.get(Field.class);
              expect(idField.getAnnotation(GeneratedValue.class)).andReturn(genval);
            })
            .expect(unit -> {
              Field idField = unit.get(Field.class);
              idField.setAccessible(true);
              idField.set(entity, 1L);

              MappedClass mclass = unit.get(MappedClass.class);
              expect(mclass.getIdField()).andReturn(idField);
              Class clazz = MonDoc.class;
              expect(mclass.getClazz()).andReturn(clazz);

              Mapper mapper = unit.get(Mapper.class);
              expect(mapper.getMappedClass(entity)).andReturn(mclass);

              Query<StoredId> query = unit.mock(Query.class);

              UpdateOperations<StoredId> uops = unit.mock(UpdateOperations.class);
              expect(uops.inc("value")).andReturn(uops);

              StoredId storedId = new StoredId();

              Datastore ds = unit.get(Datastore.class);
              expect(ds.find(StoredId.class, "_id", "Global")).andReturn(query);
              expect(ds.createUpdateOperations(StoredId.class)).andReturn(uops);
              expect(ds.findAndModify(query, uops)).andReturn(storedId);
            })
            .run(unit -> {
              new AutoIncID(unit.get(Datastore.class), IdGen.GLOBAL)
                  .prePersist(entity, unit.get(DBObject.class), unit.get(Mapper.class));
            });
  }

  @Test
  public void prePersitIgnored() throws Exception {
    Object entity = new Object();
    new MockUnit(Datastore.class, DBObject.class, Mapper.class, MappedClass.class, Field.class,
        GeneratedValue.class)
            .expect(unit -> {
              Field idField = unit.get(Field.class);
              expect(idField.getAnnotation(GeneratedValue.class)).andReturn(null);
            })
            .expect(unit -> {
              Field idField = unit.get(Field.class);

              MappedClass mclass = unit.get(MappedClass.class);
              expect(mclass.getIdField()).andReturn(idField);

              Mapper mapper = unit.get(Mapper.class);
              expect(mapper.getMappedClass(entity)).andReturn(mclass);
            })
            .run(unit -> {
              new AutoIncID(unit.get(Datastore.class), IdGen.GLOBAL)
                  .prePersist(entity, unit.get(DBObject.class), unit.get(Mapper.class));
            });
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  @Test(expected = IllegalStateException.class)
  public void prePersitErr() throws Exception {
    Object entity = new Object();
    new MockUnit(Datastore.class, DBObject.class, Mapper.class, MappedClass.class, Field.class,
        GeneratedValue.class)
            .expect(unit -> {
              GeneratedValue genval = unit.get(GeneratedValue.class);

              Field idField = unit.get(Field.class);
              expect(idField.getAnnotation(GeneratedValue.class)).andReturn(genval);
            })
            .expect(unit -> {
              Field idField = unit.get(Field.class);
              idField.setAccessible(true);
              idField.set(entity, 1L);
              expectLastCall().andThrow(new IllegalAccessException("intentional error"));

              MappedClass mclass = unit.get(MappedClass.class);
              expect(mclass.getIdField()).andReturn(idField);
              Class clazz = MonDoc.class;
              expect(mclass.getClazz()).andReturn(clazz);

              Mapper mapper = unit.get(Mapper.class);
              expect(mapper.getMappedClass(entity)).andReturn(mclass);

              Query<StoredId> query = unit.mock(Query.class);

              UpdateOperations<StoredId> uops = unit.mock(UpdateOperations.class);
              expect(uops.inc("value")).andReturn(uops);

              StoredId storedId = new StoredId();

              Datastore ds = unit.get(Datastore.class);
              expect(ds.find(StoredId.class, "_id", "Global")).andReturn(query);
              expect(ds.createUpdateOperations(StoredId.class)).andReturn(uops);
              expect(ds.findAndModify(query, uops)).andReturn(storedId);
            })
            .run(unit -> {
              new AutoIncID(unit.get(Datastore.class), IdGen.GLOBAL)
                  .prePersist(entity, unit.get(DBObject.class), unit.get(Mapper.class));
            });
  }

}
