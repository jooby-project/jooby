package org.jooby.mongodb;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.jooby.Session;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.ImmutableMap;
import com.mongodb.DBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;

@RunWith(PowerMockRunner.class)
@PrepareForTest({MongoSessionStore.class, IndexOptions.class, UpdateOptions.class, Filters.class,
    LinkedHashMap.class })
public class MongodbSessionStoreTest {

  @SuppressWarnings({"unchecked", "rawtypes" })
  MockUnit.Block boot = unit -> {
    MongoCollection collection = unit.get(MongoCollection.class);

    MongoDatabase db = unit.get(MongoDatabase.class);
    expect(db.getCollection("sess")).andReturn(collection);
  };

  long now = System.currentTimeMillis();

  Map<String, String> attrs = ImmutableMap.<String, String> of("k", "v");

  @SuppressWarnings("rawtypes")
  MockUnit.Block saveSession = unit -> {

    MongoCollection collection = unit.get(MongoCollection.class);

    Session session = unit.get(Session.class);
    expect(session.id()).andReturn("1234");
    expect(session.accessedAt()).andReturn(now);
    expect(session.createdAt()).andReturn(now);
    expect(session.savedAt()).andReturn(now);
    expect(session.attributes()).andReturn(attrs);

    UpdateResult result = unit.mock(UpdateResult.class);
    Document doc = new Document()
        .append("_id", "1234")
        .append("_accessedAt", new Date(now))
        .append("_createdAt", new Date(now))
        .append("_savedAt", new Date(now))
        .append("k", "v");

    UpdateOptions options = unit.constructor(UpdateOptions.class)
        .build();
    expect(options.upsert(true)).andReturn(options);

    Bson eq = unit.mock(Bson.class);
    unit.mockStatic(Filters.class);
    expect(Filters.eq("_id", "1234")).andReturn(eq);

    expect(collection.updateOne(eq, new Document("$set", doc), options))
        .andReturn(result);
  };

  @SuppressWarnings("rawtypes")
  private Block noIndexes = unit -> {
    MongoCursor cursor = unit.mock(MongoCursor.class);
    expect(cursor.hasNext()).andReturn(false);

    ListIndexesIterable lii = unit.mock(ListIndexesIterable.class);
    expect(lii.iterator()).andReturn(cursor);

    MongoCollection coll = unit.get(MongoCollection.class);
    expect(coll.listIndexes()).andReturn(lii);
  };

  @SuppressWarnings("rawtypes")
  private Block indexes = unit -> {

    Document d1 = unit.mock(Document.class);
    expect(d1.getString("name")).andReturn("n1");

    Document d2 = unit.mock(Document.class);
    expect(d2.getString("name")).andReturn("_sessionIdx_");

    MongoCursor cursor = unit.mock(MongoCursor.class);
    expect(cursor.hasNext()).andReturn(true);
    expect(cursor.next()).andReturn(d1);
    expect(cursor.hasNext()).andReturn(true);
    expect(cursor.next()).andReturn(d2);

    ListIndexesIterable lii = unit.mock(ListIndexesIterable.class);
    expect(lii.iterator()).andReturn(cursor);

    MongoCollection coll = unit.get(MongoCollection.class);
    expect(coll.listIndexes()).andReturn(lii);
  };

  private Block runCommand = unit -> {
    MongoDatabase db = unit.get(MongoDatabase.class);
    Document command = new Document("collMod", "sess")
        .append("index",
            new Document("keyPattern", new Document("_accessedAt", 1))
                .append("expireAfterSeconds", 60L));

    expect(db.runCommand(command)).andReturn(unit.mock(Document.class));
  };

  @Test
  public void defaults() throws Exception {
    new MockUnit(MongoDatabase.class, MongoCollection.class)
        .expect(boot)
        .run(unit -> {
          new MongoSessionStore(unit.get(MongoDatabase.class), "sess", "1m");
        });
  }

  @Test(expected = NullPointerException.class)
  public void defaultsNullDB() throws Exception {
    new MongoSessionStore(null, "sess", "1m");
  }

  @SuppressWarnings("rawtypes")
  @Test
  public void create() throws Exception {
    new MockUnit(Session.class, MongoDatabase.class, MongoCollection.class)
        .expect(boot)
        .expect(saveSession)
        .expect(noIndexes)
        .expect(unit -> {
          MongoCollection collection = unit.get(MongoCollection.class);
          IndexOptions options = unit.constructor(IndexOptions.class)
              .build();
          expect(options.name("_sessionIdx_")).andReturn(options);
          expect(options.expireAfter(300L, TimeUnit.SECONDS)).andReturn(options);
          expect(collection.createIndex(new Document("_accessedAt", 1), options))
              .andReturn("idx");
        })
        .run(unit -> {
          new MongoSessionStore(unit.get(MongoDatabase.class), "sess", "5m")
              .create(unit.get(Session.class));
          ;
        });
  }

  @Test
  public void save() throws Exception {
    new MockUnit(Session.class, MongoDatabase.class, MongoCollection.class)
        .expect(boot)
        .expect(indexes)
        .expect(saveSession)
        .expect(runCommand)
        .run(unit -> {
          new MongoSessionStore(unit.get(MongoDatabase.class), "sess", "60")
              .save(unit.get(Session.class));
          ;
        });
  }

  @SuppressWarnings("rawtypes")
  @Test
  public void shouldSyncTtlOnce() throws Exception {
    new MockUnit(Session.class, MongoDatabase.class, MongoCollection.class)
        .expect(boot)
        .expect(saveSession)
        .expect(noIndexes)
        .expect(saveSession)
        .expect(unit -> {
          MongoCollection collection = unit.get(MongoCollection.class);
          IndexOptions options = unit.constructor(IndexOptions.class)
              .build();
          expect(options.name("_sessionIdx_")).andReturn(options);
          expect(options.expireAfter(60L, TimeUnit.SECONDS)).andReturn(options);
          expect(collection.createIndex(new Document("_accessedAt", 1), options))
              .andReturn("idx");
        })
        .run(unit -> {
          MongoSessionStore mongodb = new MongoSessionStore(unit.get(MongoDatabase.class), "sess",
              "60");
          mongodb.save(unit.get(Session.class));
          mongodb.save(unit.get(Session.class));
        });
  }

  @Test
  public void saveNoTimeout() throws Exception {
    new MockUnit(Session.class, MongoDatabase.class, MongoCollection.class)
        .expect(boot)
        .expect(saveSession)
        .run(unit -> {
          new MongoSessionStore(unit.get(MongoDatabase.class), "sess", "0")
              .save(unit.get(Session.class));
          ;
        });
  }

  @Test
  public void saveSyncTtl() throws Exception {
    new MockUnit(Session.class, MongoDatabase.class, MongoCollection.class)
        .expect(boot)
        .expect(saveSession)
        .expect(indexes)
        .expect(runCommand)
        .run(unit -> {
          new MongoSessionStore(unit.get(MongoDatabase.class), "sess", "60")
              .save(unit.get(Session.class));
          ;
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void get() throws Exception {
    long now = System.currentTimeMillis();
    new MockUnit(Session.class, Session.Builder.class, MongoDatabase.class, MongoCollection.class,
        DBObject.class)
            .expect(boot)
            .expect(unit -> {
              Document doc = unit.mock(Document.class);

              Map sessionMap = unit.constructor(LinkedHashMap.class)
                  .args(Map.class)
                  .build(doc);
              expect(sessionMap.remove("_accessedAt")).andReturn(new Date(now));
              expect(sessionMap.remove("_createdAt")).andReturn(new Date(now));
              expect(sessionMap.remove("_savedAt")).andReturn(new Date(now));
              expect(sessionMap.remove("_id")).andReturn("1234");

              FindIterable result = unit.mock(FindIterable.class);
              expect(result.first()).andReturn(doc);

              Session.Builder sb = unit.get(Session.Builder.class);
              expect(sb.sessionId()).andReturn("1234");
              expect(sb.accessedAt(now)).andReturn(sb);
              expect(sb.createdAt(now)).andReturn(sb);
              expect(sb.savedAt(now)).andReturn(sb);
              expect(sb.set(sessionMap)).andReturn(sb);
              expect(sb.build()).andReturn(unit.get(Session.class));

              Bson eq = unit.mock(Bson.class);
              unit.mockStatic(Filters.class);
              expect(Filters.eq("_id", "1234")).andReturn(eq);

              MongoCollection collection = unit.get(MongoCollection.class);
              expect(collection.find(eq)).andReturn(result);
            })
            .run(unit -> {
              MongoSessionStore mss = new MongoSessionStore(unit.get(MongoDatabase.class), "sess",
                  "60");
              assertEquals(unit.get(Session.class), mss.get(unit.get(Session.Builder.class)));
            });
  }

  @SuppressWarnings("rawtypes")
  @Test
  public void getExpired() throws Exception {
    new MockUnit(Session.class, Session.Builder.class, MongoDatabase.class, MongoCollection.class)
        .expect(boot)
        .expect(unit -> {
          Session.Builder sb = unit.get(Session.Builder.class);
          expect(sb.sessionId()).andReturn("1234");

          Bson eq = unit.mock(Bson.class);
          unit.mockStatic(Filters.class);
          expect(Filters.eq("_id", "1234")).andReturn(eq);

          FindIterable result = unit.mock(FindIterable.class);
          expect(result.first()).andReturn(null);

          MongoCollection collection = unit.get(MongoCollection.class);
          expect(collection.find(eq)).andReturn(result);
        })
        .run(unit -> {
          assertEquals(null,
              new MongoSessionStore(unit.get(MongoDatabase.class), "sess", "60")
                  .get(unit.get(Session.Builder.class)));
        });
  }

  @SuppressWarnings("rawtypes")
  @Test
  public void delete() throws Exception {
    new MockUnit(MongoDatabase.class, MongoCollection.class)
        .expect(boot)
        .expect(unit -> {
          MongoCollection collection = unit.get(MongoCollection.class);
          expect(collection.deleteOne(new Document("_id", "1234"))).andReturn(null);
        })
        .run(unit -> {
          new MongoSessionStore(unit.get(MongoDatabase.class), "sess", "60")
              .delete("1234");
        });
  }

}
