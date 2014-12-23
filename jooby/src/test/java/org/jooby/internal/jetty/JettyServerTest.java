package org.jooby.internal.jetty;

import static org.easymock.EasyMock.expect;

import org.eclipse.jetty.server.Server;
import org.jooby.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.inject.Injector;


@RunWith(PowerMockRunner.class)
@PrepareForTest({JettyServer.class, JettyServerBuilder.class, Server.class })
public class JettyServerTest {

  @Test
  public void start() throws Exception {
    new MockUnit(Injector.class)
        .expect(unit -> {
          unit.mockStatic(JettyServerBuilder.class);

          Server server = unit.partialMock(Server.class, "start", "join");
          server.start();
          server.join();

          expect(JettyServerBuilder.build(unit.get(Injector.class))).andReturn(server);
        })
        .run(unit -> {
          new JettyServer(unit.get(Injector.class)).start();
        });
  }

  @Test
  public void stop() throws Exception {
    new MockUnit(Injector.class)
        .expect(unit -> {
          unit.mockStatic(JettyServerBuilder.class);

          Server server = unit.partialMock(Server.class, "stop");
          server.stop();

          expect(JettyServerBuilder.build(unit.get(Injector.class))).andReturn(server);
        })
        .run(unit -> {
          new JettyServer(unit.get(Injector.class)).stop();
        });
  }
}
