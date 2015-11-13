package org.jooby.internal.ebean;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.EbeanServerFactory;
import com.avaje.ebean.config.ServerConfig;
import com.typesafe.config.Config;

@RunWith(PowerMockRunner.class)
@PrepareForTest({EbeanManaged.class, EbeanServerFactory.class })
public class EbeanManagedTest {

  private Block createEbean = unit -> {
    EbeanServer ebean = unit.get(EbeanServer.class);

    unit.mockStatic(EbeanServerFactory.class);
    expect(EbeanServerFactory.create(unit.get(ServerConfig.class))).andReturn(ebean);
  };

  private Block withoutDdl = unit -> {
    ServerConfig conf = unit.get(ServerConfig.class);
    expect(conf.isDdlGenerate()).andReturn(false);
  };

  private Block withDdl = unit -> {
    ServerConfig sconf = unit.get(ServerConfig.class);
    expect(sconf.isDdlGenerate()).andReturn(true);
    expect(sconf.getName()).andReturn("db").times(2);

    Config conf = unit.get(Config.class);
    expect(conf.getString("application.tmpdir")).andReturn("target");
  };

  @Test
  public void newEbeanManaged() throws Exception {
    new MockUnit(Config.class, ServerConfig.class)
        .run(unit -> {
          new EbeanManaged(unit.get(Config.class), unit.get(ServerConfig.class));
        });
  }

  @Test
  public void start() throws Exception {
    new MockUnit(Config.class, ServerConfig.class, EbeanServer.class)
        .expect(createEbean)
        .expect(withoutDdl)
        .run(unit -> {
          new EbeanManaged(unit.get(Config.class), unit.get(ServerConfig.class)).start();
        });
  }

  @Test
  public void startWithDdl() throws Exception {
    new MockUnit(Config.class, ServerConfig.class, EbeanServer.class)
        .expect(createEbean)
        .expect(withDdl)
        .run(unit -> {
          new EbeanManaged(unit.get(Config.class), unit.get(ServerConfig.class)).start();
        });
  }

  @Test
  public void startShouldIgnoreOn2ndCall() throws Exception {
    new MockUnit(Config.class, ServerConfig.class, EbeanServer.class)
        .expect(createEbean)
        .expect(withDdl)
        .run(unit -> {
          EbeanManaged managed = new EbeanManaged(unit.get(Config.class),
              unit.get(ServerConfig.class));
          managed.start();
          // ignored
          managed.start();
        });
  }

  @Test
  public void get() throws Exception {
    new MockUnit(Config.class, ServerConfig.class, EbeanServer.class)
        .expect(createEbean)
        .expect(withoutDdl)
        .run(unit -> {
          EbeanManaged managed = new EbeanManaged(unit.get(Config.class),
              unit.get(ServerConfig.class));
          assertEquals(unit.get(EbeanServer.class), managed.get());
          assertEquals(unit.get(EbeanServer.class), managed.get());
        });
  }

  @Test
  public void stop() throws Exception {
    new MockUnit(Config.class, ServerConfig.class, EbeanServer.class)
        .expect(createEbean)
        .expect(withoutDdl)
        .expect(unit -> {
          EbeanServer ebean = unit.get(EbeanServer.class);
          ebean.shutdown(false, false);
        })
        .run(unit -> {
          EbeanManaged managed = new EbeanManaged(unit.get(Config.class),
              unit.get(ServerConfig.class));
          managed.stop();
          // ignored
          managed.stop();
        });
  }

}
