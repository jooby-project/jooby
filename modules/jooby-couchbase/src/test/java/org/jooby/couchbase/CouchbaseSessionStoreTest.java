package org.jooby.couchbase;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.jooby.Session;
import org.jooby.Session.Builder;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.google.common.collect.ImmutableMap;

public class CouchbaseSessionStoreTest {

  @Test
  public void createDocument() throws Exception {
    new MockUnit(Bucket.class, Session.class)
        .expect(session("sid", 2, 3, 4, "foo", "bar"))
        .expect(unit -> {
          Bucket bucket = unit.get(Bucket.class);
          JsonDocument doc = JsonDocument.create("session::sid", 60, JsonObject.create()
              .put("foo", "bar")
              .put("_createdAt", 2L)
              .put("_accessedAt", 3L)
              .put("_savedAt", 4L));
          expect(bucket.upsert(doc)).andReturn(doc);
        })
        .run(unit -> {
          new CouchbaseSessionStore(unit.get(Bucket.class), "1m")
              .create(unit.get(Session.class));
        });
  }

  @Test
  public void getDocument() throws Exception {
    new MockUnit(Bucket.class, Session.Builder.class, Session.class)
        .expect(unit -> {
          JsonDocument doc = JsonDocument.create("session::sid", 60, JsonObject.create()
              .put("foo", "bar")
              .put("_createdAt", 2L)
              .put("_accessedAt", 3L)
              .put("_savedAt", 4L));

          Builder builder = unit.get(Session.Builder.class);
          expect(builder.sessionId()).andReturn("sid");
          expect(builder.accessedAt(3L)).andReturn(builder);
          expect(builder.createdAt(2L)).andReturn(builder);
          expect(builder.savedAt(4L)).andReturn(builder);
          expect(builder.set(ImmutableMap.of("foo", "bar"))).andReturn(builder);
          expect(builder.build()).andReturn(unit.get(Session.class));

          Bucket bucket = unit.get(Bucket.class);
          expect(bucket.getAndTouch("session::sid", 60)).andReturn(doc);
        })
        .run(unit -> {
          Session session = new CouchbaseSessionStore(unit.get(Bucket.class), "1m")
              .get(unit.get(Session.Builder.class));
          assertEquals(unit.get(Session.class), session);
        });
  }

  @Test
  public void deleteDocument() throws Exception {
    new MockUnit(Bucket.class, Session.Builder.class, Session.class)
        .expect(unit -> {
          Bucket bucket = unit.get(Bucket.class);
          expect(bucket.remove("session::rid")).andReturn(null);
        })
        .run(unit -> {
          new CouchbaseSessionStore(unit.get(Bucket.class), "1m")
              .delete("rid");
        });
  }

  @Test
  public void saveDocument() throws Exception {
    new MockUnit(Bucket.class, Session.class)
        .expect(session("sid", 2, 3, 4, "foo", "bar"))
        .expect(unit -> {
          Bucket bucket = unit.get(Bucket.class);
          JsonDocument doc = JsonDocument.create("session::sid", 60, JsonObject.create()
              .put("foo", "bar")
              .put("_createdAt", 2L)
              .put("_accessedAt", 3L)
              .put("_savedAt", 4L));
          expect(bucket.upsert(doc)).andReturn(doc);
        })
        .run(unit -> {
          new CouchbaseSessionStore(unit.get(Bucket.class), "1m")
              .save(unit.get(Session.class));
        });
  }

  @Test
  public void createDocumentWithSeconds() throws Exception {
    new MockUnit(Bucket.class, Session.class)
        .expect(session("sid", 2, 3, 4, "foo", "bar"))
        .expect(unit -> {
          Bucket bucket = unit.get(Bucket.class);
          JsonDocument doc = JsonDocument.create("session::sid", 45, JsonObject.create()
              .put("foo", "bar")
              .put("_createdAt", 2L)
              .put("_accessedAt", 3L)
              .put("_savedAt", 4L));
          expect(bucket.upsert(doc)).andReturn(doc);
        })
        .run(unit -> {
          new CouchbaseSessionStore(unit.get(Bucket.class), "45")
              .create(unit.get(Session.class));
        });
  }

  @Test
  public void createDocumentNoTimeout() throws Exception {
    new MockUnit(Bucket.class, Session.class)
        .expect(session("sid", 2, 3, 4, "foo", "bar"))
        .expect(unit -> {
          Bucket bucket = unit.get(Bucket.class);
          JsonDocument doc = JsonDocument.create("session::sid", 0, JsonObject.create()
              .put("foo", "bar")
              .put("_createdAt", 2L)
              .put("_accessedAt", 3L)
              .put("_savedAt", 4L));
          expect(bucket.upsert(doc)).andReturn(doc);
        })
        .run(unit -> {
          new CouchbaseSessionStore(unit.get(Bucket.class), -1)
              .create(unit.get(Session.class));
        });
  }

  private Block session(final String id, final long createdAt, final long accessedAt,
      final long savedAt,
      final String... attributes) {
    return unit -> {
      Session session = unit.get(Session.class);
      Map<String, String> hash = new HashMap<>();
      for (int i = 0; i < attributes.length; i += 2) {
        hash.put(attributes[i], attributes[i + 1]);
      }
      expect(session.attributes()).andReturn(hash);
      expect(session.accessedAt()).andReturn(accessedAt);
      expect(session.createdAt()).andReturn(createdAt);
      expect(session.savedAt()).andReturn(savedAt);
      expect(session.id()).andReturn(id);
    };
  }
}
