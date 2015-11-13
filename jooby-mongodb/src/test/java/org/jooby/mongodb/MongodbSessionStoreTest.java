package org.jooby.mongodb;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.Map;

import org.jooby.Session;
import org.jooby.test.MockUnit;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.WriteResult;

public class MongodbSessionStoreTest {

  MockUnit.Block boot = unit -> {
    DBCollection collection = unit.get(DBCollection.class);

    DB db = unit.get(DB.class);
    expect(db.getCollection("sess")).andReturn(collection);
  };

  long now = System.currentTimeMillis();

  Map<String, String> attrs = ImmutableMap.<String, String> of("k", "v");

  MockUnit.Block saveSession = unit -> {

    DBCollection collection = unit.get(DBCollection.class);

    Session session = unit.get(Session.class);
    expect(session.id()).andReturn("1234");
    expect(session.accessedAt()).andReturn(now);
    expect(session.createdAt()).andReturn(now);
    expect(session.savedAt()).andReturn(now);
    expect(session.attributes()).andReturn(attrs);

    WriteResult result = unit.mock(WriteResult.class);
    expect(collection.save(BasicDBObjectBuilder.start()
        .add("_id", "1234")
        .add("_accessedAt", new Date(now))
        .add("_createdAt", new Date(now))
        .add("_savedAt", new Date(now))
        .add("k", "v")
        .get()
        )).andReturn(result);
  };

  @Test
  public void defaults() throws Exception {
    new MockUnit(DB.class, DBCollection.class)
        .expect(boot)
        .run(unit -> {
          new MongoSessionStore(unit.get(DB.class), "sess", "1m");
        });
  }

  @Test(expected = NullPointerException.class)
  public void defaultsNullDB() throws Exception {
    new MongoSessionStore(null, "sess", "1m");
  }

  @Test
  public void create() throws Exception {
    new MockUnit(Session.class, DB.class, DBCollection.class)
        .expect(boot)
        .expect(saveSession)
        .expect(unit -> {
          DBCollection collection = unit.get(DBCollection.class);
          collection.createIndex(new BasicDBObject("_accessedAt", 1), new BasicDBObject(
              "expireAfterSeconds", 300));
        })
        .run(unit -> {
          new MongoSessionStore(unit.get(DB.class), "sess", "5m")
              .create(unit.get(Session.class));
          ;
        });
  }

  @Test
  public void save() throws Exception {
    new MockUnit(Session.class, DB.class, DBCollection.class)
        .expect(boot)
        .expect(saveSession)
        .expect(unit -> {
          DBCollection collection = unit.get(DBCollection.class);
          collection.createIndex(new BasicDBObject("_accessedAt", 1), new BasicDBObject(
              "expireAfterSeconds", 60));
        })
        .run(unit -> {
          new MongoSessionStore(unit.get(DB.class), "sess", "60")
              .save(unit.get(Session.class));
          ;
        });
  }

  @Test
  public void shouldSyncTtlOnce() throws Exception {
    new MockUnit(Session.class, DB.class, DBCollection.class)
        .expect(boot)
        .expect(saveSession)
        .expect(saveSession)
        .expect(unit -> {
          DBCollection collection = unit.get(DBCollection.class);
          collection.createIndex(new BasicDBObject("_accessedAt", 1), new BasicDBObject(
              "expireAfterSeconds", 60));
        })
        .run(unit -> {
          MongoSessionStore mongodb = new MongoSessionStore(unit.get(DB.class), "sess", "60");
          mongodb.save(unit.get(Session.class));
          mongodb.save(unit.get(Session.class));
        });
  }

  @Test
  public void saveNoTimeout() throws Exception {
    new MockUnit(Session.class, DB.class, DBCollection.class)
        .expect(boot)
        .expect(saveSession)
        .run(unit -> {
          new MongoSessionStore(unit.get(DB.class), "sess", "0")
              .save(unit.get(Session.class));
          ;
        });
  }

  @Test
  public void saveSyncTtl() throws Exception {
    new MockUnit(Session.class, DB.class, DBCollection.class)
        .expect(boot)
        .expect(saveSession)
        .expect(unit -> {
          DBCollection collection = unit.get(DBCollection.class);
          collection.createIndex(new BasicDBObject("_accessedAt", 1), new BasicDBObject(
              "expireAfterSeconds", 60));
          expectLastCall().andThrow(new MongoException("intentional err"));

          DB db = unit.get(DB.class);

          expect(db.command(BasicDBObjectBuilder.start()
              .add("collMod", "sess")
              .add("index", BasicDBObjectBuilder.start()
                  .add("keyPattern", new BasicDBObject("_accessedAt", 1))
                  .add("expireAfterSeconds", 60)
                  .get()
              )
              .get())
            ).andReturn(null);
          })
        .run(unit -> {
          new MongoSessionStore(unit.get(DB.class), "sess", "60")
              .save(unit.get(Session.class));
          ;
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void get() throws Exception {
    long now = System.currentTimeMillis();
    new MockUnit(Session.class, Session.Builder.class, DB.class, DBCollection.class, DBObject.class)
        .expect(boot)
        .expect(unit -> {
          Map sessionMap = unit.mock(Map.class);
          expect(sessionMap.remove("_accessedAt")).andReturn(new Date(now));
          expect(sessionMap.remove("_createdAt")).andReturn(new Date(now));
          expect(sessionMap.remove("_savedAt")).andReturn(new Date(now));
          expect(sessionMap.remove("_id")).andReturn("1234");

          DBObject dbobj = unit.get(DBObject.class);
          expect(dbobj.toMap()).andReturn(sessionMap);

          Session.Builder sb = unit.get(Session.Builder.class);
          expect(sb.sessionId()).andReturn("1234");
          expect(sb.accessedAt(now)).andReturn(sb);
          expect(sb.createdAt(now)).andReturn(sb);
          expect(sb.savedAt(now)).andReturn(sb);
          expect(sb.set(sessionMap)).andReturn(sb);
          expect(sb.build()).andReturn(unit.get(Session.class));

          DBCollection collection = unit.get(DBCollection.class);
          expect(collection.findOne("1234")).andReturn(dbobj);
        })
        .run(unit -> {
          MongoSessionStore mss = new MongoSessionStore(unit.get(DB.class), "sess", "60");
          assertEquals(unit.get(Session.class), mss.get(unit.get(Session.Builder.class)));
        });
  }

  @Test
  public void getExpired() throws Exception {
    new MockUnit(Session.class, Session.Builder.class, DB.class, DBCollection.class)
        .expect(boot)
        .expect(unit -> {

          Session.Builder sb = unit.get(Session.Builder.class);
          expect(sb.sessionId()).andReturn("1234");

          DBCollection collection = unit.get(DBCollection.class);
          expect(collection.findOne("1234")).andReturn(null);
        })
        .run(unit -> {
          assertEquals(null,
              new MongoSessionStore(unit.get(DB.class), "sess", "60")
                  .get(unit.get(Session.Builder.class)));
        });
  }

  @Test
  public void delete() throws Exception {
    new MockUnit(DB.class, DBCollection.class)
        .expect(boot)
        .expect(unit -> {
          DBCollection collection = unit.get(DBCollection.class);
          expect(collection.remove(new BasicDBObject("_id", "1234"))).andReturn(null);
        })
        .run(unit -> {
          new MongoSessionStore(unit.get(DB.class), "sess", "60")
              .delete("1234");
        });
  }

}
