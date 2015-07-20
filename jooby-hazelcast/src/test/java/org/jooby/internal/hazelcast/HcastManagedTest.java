package org.jooby.internal.hazelcast;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

@RunWith(PowerMockRunner.class)
@PrepareForTest({HcastManaged.class, Hazelcast.class })
public class HcastManagedTest {

  private Block start = unit -> {
    unit.mockStatic(Hazelcast.class);
    expect(Hazelcast.newHazelcastInstance(unit.get(Config.class))).andReturn(
        unit.get(HazelcastInstance.class));
  };

  @Test
  public void defaults() throws Exception {
    new MockUnit(Config.class)
        .run(unit -> {
          new HcastManaged(unit.get(Config.class));
        });
  }

  @Test
  public void start() throws Exception {
    new MockUnit(Config.class, HazelcastInstance.class)
        .expect(start)
        .run(unit -> {
          new HcastManaged(unit.get(Config.class))
              .start();
          ;
        });
  }

  @Test
  public void stop() throws Exception {
    new MockUnit(Config.class, HazelcastInstance.class)
        .expect(start)
        .expect(unit -> {
          HazelcastInstance hcast = unit.get(HazelcastInstance.class);
          hcast.shutdown();
        })
        .run(unit -> {
          HcastManaged hcast = new HcastManaged(unit.get(Config.class));
          hcast.start();
          hcast.stop();
          hcast.stop();
          ;
        });
  }

  @Test
  public void get() throws Exception {
    new MockUnit(Config.class, HazelcastInstance.class)
        .expect(start)
        .run(unit -> {
          HcastManaged hcast = new HcastManaged(unit.get(Config.class));
          hcast.start();
          assertEquals(unit.get(HazelcastInstance.class), hcast.get());
        });
  }

}
