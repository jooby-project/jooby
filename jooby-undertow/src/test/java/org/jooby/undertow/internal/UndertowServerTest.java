package org.jooby.undertow.internal;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import io.undertow.Undertow.Builder;

import org.jooby.MockUnit;
import org.jooby.undertow.internal.UndertowServer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.xnio.Options;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

@RunWith(PowerMockRunner.class)
@PrepareForTest({UndertowServer.class, io.undertow.Undertow.class,
    io.undertow.Undertow.Builder.class })
public class UndertowServerTest {

  @Test
  public void configure() throws Exception {
    new MockUnit(Builder.class, Config.class)
        .expect(unit -> {
          Config undertow = unit.mock(Config.class);
          expect(undertow.hasPath("bufferSize")).andReturn(true);
          expect(undertow.getInt("bufferSize")).andReturn(1234);

          expect(undertow.hasPath("buffersPerRegion")).andReturn(true);
          expect(undertow.getInt("buffersPerRegion")).andReturn(10);

          expect(undertow.hasPath("directBuffers")).andReturn(true);
          expect(undertow.getBoolean("directBuffers")).andReturn(true);

          expect(undertow.hasPath("ioThreads")).andReturn(true);
          expect(undertow.getInt("ioThreads")).andReturn(2);

          expect(undertow.hasPath("workerThreads")).andReturn(true);
          expect(undertow.getInt("workerThreads")).andReturn(5);

          Config config = unit.get(Config.class);
          expect(config.getConfig("undertow")).andReturn(undertow);

          Config server = ConfigFactory.empty()
              .withValue("REUSE_ADDRESSES", ConfigValueFactory.fromAnyRef(true));
          expect(undertow.getConfig("server")).andReturn(server);

          Config worker = ConfigFactory.empty()
              .withValue("TCP_NODELAY", ConfigValueFactory.fromAnyRef(true));
          expect(undertow.getConfig("worker")).andReturn(worker);

          Config socket = ConfigFactory.empty()
              .withValue("IGNORE_INVALID", ConfigValueFactory.fromAnyRef(true))
              .withValue("KEEP_ALIVE", ConfigValueFactory.fromAnyRef(true));

          expect(undertow.getConfig("socket")).andReturn(socket);
        })
        .expect(unit -> {
          Builder builder = unit.get(Builder.class);

          expect(builder.setBufferSize(1234)).andReturn(builder);
          expect(builder.setBuffersPerRegion(10)).andReturn(builder);
          expect(builder.setDirectBuffers(true)).andReturn(builder);
          expect(builder.setIoThreads(2)).andReturn(builder);
          expect(builder.setWorkerThreads(5)).andReturn(builder);

          expect(builder.setServerOption(Options.REUSE_ADDRESSES, true)).andReturn(builder);
          expect(builder.setWorkerOption(Options.TCP_NODELAY, true)).andReturn(builder);
          expect(builder.setSocketOption(Options.KEEP_ALIVE, true)).andReturn(builder);
        })
        .run(unit -> {
          Builder builder = UndertowServer
              .configure(unit.get(Config.class), unit.get(Builder.class));
          assertEquals(unit.get(Builder.class), builder);
        });
  }
}
