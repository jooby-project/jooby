package org.jooby.mongodb;

import static org.junit.Assert.assertNotNull;

import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

@RunWith(PowerMockRunner.class)
@PrepareForTest({MongodbManaged.class, MongoClient.class })
public class MongodbManagedTest {

  @Test
  public void start() throws Exception {
    MongoClientURI uri = new MongoClientURI("mongodb://127.0.0.1");
    new MockUnit()
        .run(unit -> {
          MongodbManaged managed = new MongodbManaged(uri);
          managed.start();
        });
  }

  @Test
  public void get() throws Exception {
    MongoClientURI uri = new MongoClientURI("mongodb://127.0.0.1");
    new MockUnit()
        .expect(unit -> {
          unit.mockConstructor(MongoClient.class, new Class[]{MongoClientURI.class }, uri);
        })
        .run(unit -> {
          MongodbManaged managed = new MongodbManaged(uri);
          managed.start();
          assertNotNull(managed.get());
        });
  }

  @Test
  public void stop() throws Exception {
    MongoClientURI uri = new MongoClientURI("mongodb://127.0.0.1");
    new MockUnit()
        .expect(unit -> {
          MongoClient client = unit.mockConstructor(MongoClient.class,
              new Class[]{MongoClientURI.class }, uri);
          client.close();
        })
        .run(unit -> {
          MongodbManaged managed = new MongodbManaged(uri);
          managed.get();
          managed.stop();
        });
  }

  @Test
  public void stopIsIgnoredAfter1stCall() throws Exception {
    MongoClientURI uri = new MongoClientURI("mongodb://127.0.0.1");
    new MockUnit()
        .expect(unit -> {
          MongoClient client = unit.mockConstructor(MongoClient.class,
              new Class[]{MongoClientURI.class }, uri);
          client.close();
        })
        .run(unit -> {
          MongodbManaged managed = new MongodbManaged(uri);
          managed.get();
          managed.stop();
          managed.stop();
        });
  }

}
