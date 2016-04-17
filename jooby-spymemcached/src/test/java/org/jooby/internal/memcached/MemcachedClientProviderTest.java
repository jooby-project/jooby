package org.jooby.internal.memcached;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.ConnectionFactory;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.MemcachedClient;

@RunWith(PowerMockRunner.class)
@PrepareForTest({MemcachedClientProvider.class, MemcachedClient.class })
public class MemcachedClientProviderTest {

  private Block start = unit -> {
    ConnectionFactory cf = unit.mock(ConnectionFactory.class);

    MemcachedClient client = unit.mockConstructor(MemcachedClient.class, new Class[]{
        ConnectionFactory.class, List.class }, eq(cf), isA(List.class));
    unit.registerMock(MemcachedClient.class, client);

    ConnectionFactoryBuilder cfb = unit.get(ConnectionFactoryBuilder.class);
    expect(cfb.build()).andReturn(cf);
  };

  @Test
  public void defaults() throws Exception {
    List<InetSocketAddress> servers = AddrUtil.getAddresses("localhost:11211");
    long timeout = -1;
    new MockUnit(ConnectionFactoryBuilder.class)
        .run(unit -> {
          new MemcachedClientProvider(unit.get(ConnectionFactoryBuilder.class), servers, timeout);
        });
  }

  @Test
  public void get() throws Exception {
    List<InetSocketAddress> servers = AddrUtil.getAddresses("localhost:11211");
    long timeout = -1;
    new MockUnit(ConnectionFactoryBuilder.class)
        .expect(start)
        .run(unit -> {
          new MemcachedClientProvider(unit.get(ConnectionFactoryBuilder.class), servers, timeout)
              .get();
        });
  }

  @Test
  public void stop() throws Exception {
    List<InetSocketAddress> servers = AddrUtil.getAddresses("localhost:11211");
    long timeout = -1;
    new MockUnit(ConnectionFactoryBuilder.class)
        .expect(start)
        .expect(unit -> {
          MemcachedClient client = unit.get(MemcachedClient.class);
          expect(client.shutdown(timeout, TimeUnit.MILLISECONDS)).andReturn(true);
        })
        .run(unit -> {
          MemcachedClientProvider client = new MemcachedClientProvider(unit
              .get(ConnectionFactoryBuilder.class), servers, timeout);
          client.get();
          client.destroy();
          client.destroy();
        });
  }

}
