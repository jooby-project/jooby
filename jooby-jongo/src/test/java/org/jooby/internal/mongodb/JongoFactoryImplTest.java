package org.jooby.internal.mongodb;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import org.jongo.Jongo;
import org.jongo.Mapper;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

@RunWith(PowerMockRunner.class)
@PrepareForTest({JongoFactoryImpl.class, Jongo.class })
public class JongoFactoryImplTest {

  private String db = "db";

  private Block database = unit -> {
    MongoClientURI uri = unit.get(MongoClientURI.class);
    expect(uri.getDatabase()).andReturn(db);
  };

  private Block jongo = unit -> {
    DB database = unit.get(DB.class);
    Mapper mapper = unit.get(Mapper.class);

    Jongo jongo = unit.mockConstructor(Jongo.class, new Class[]{DB.class, Mapper.class }, database,
        mapper);

    unit.registerMock(Jongo.class, jongo);
  };

  @SuppressWarnings("deprecation")
  private Block defdb = unit -> {
    MongoClient client = unit.get(MongoClient.class);
    DB database = unit.get(DB.class);
    expect(client.getDB(db)).andReturn(database);
  };

  @SuppressWarnings("deprecation")
  private Block customdb = unit -> {
    MongoClient client = unit.get(MongoClient.class);
    DB database = unit.get(DB.class);
    expect(client.getDB("xdb")).andReturn(database);
  };

  @Test
  public void defaults() throws Exception {
    new MockUnit(MongoClientURI.class, MongoClient.class, Mapper.class)
        .expect(database)
        .run(unit -> {
          new JongoFactoryImpl(
              unit.get(MongoClientURI.class),
              unit.get(MongoClient.class),
              unit.get(Mapper.class)
            );
          });
  }

  @Test
  public void get() throws Exception {
    new MockUnit(MongoClientURI.class, MongoClient.class, Mapper.class, DB.class)
        .expect(database)
        .expect(defdb)
        .expect(jongo)
        .run(unit -> {
          Jongo jongo = new JongoFactoryImpl(
              unit.get(MongoClientURI.class),
              unit.get(MongoClient.class),
              unit.get(Mapper.class)
              ).get();
          assertEquals(unit.get(Jongo.class), jongo);
        });
  }

  @Test
  public void getCustomDb() throws Exception {
    new MockUnit(MongoClientURI.class, MongoClient.class, Mapper.class, DB.class)
        .expect(database)
        .expect(customdb)
        .expect(jongo)
        .run(unit -> {
          Jongo jongo = new JongoFactoryImpl(
              unit.get(MongoClientURI.class),
              unit.get(MongoClient.class),
              unit.get(Mapper.class)
              ).get("xdb");
          assertEquals(unit.get(Jongo.class), jongo);
        });
  }

}
