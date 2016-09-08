package org.jooby.internal.undertow;

import static org.easymock.EasyMock.expect;

import java.io.IOException;

import org.jooby.spi.NativeResponse;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.undertow.io.IoCallback;
import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;

@RunWith(PowerMockRunner.class)
@PrepareForTest({LogIoCallback.class, HttpServerExchange.class, LoggerFactory.class })
public class LogIoCallbackTest {

  @Test
  public void onComplete() throws Exception {
    new MockUnit(IoCallback.class, HttpServerExchange.class, Sender.class)
        .expect(unit -> {
          unit.get(IoCallback.class).onComplete(unit.get(HttpServerExchange.class),
              unit.get(Sender.class));
        })
        .run(unit -> {
          new LogIoCallback(unit.get(IoCallback.class))
              .onComplete(unit.get(HttpServerExchange.class), unit.get(Sender.class));
        });
  }

  @Test
  public void onException() throws Exception {
    IOException x = new IOException("intentional err");
    new MockUnit(IoCallback.class, HttpServerExchange.class, Sender.class, Logger.class)
        .expect(unit -> {
          unit.mockStatic(LoggerFactory.class);

          Logger log = unit.get(Logger.class);
          log.error("execution of {} resulted in exception", "/assets/main.js", x);

          expect(LoggerFactory.getLogger(NativeResponse.class)).andReturn(log);
          HttpServerExchange exchange = unit.get(HttpServerExchange.class);
          expect(exchange.getRequestPath()).andReturn("/assets/main.js");
          unit.get(IoCallback.class).onException(exchange, unit.get(Sender.class), x);
        })
        .run(unit -> {
          new LogIoCallback(unit.get(IoCallback.class))
              .onException(unit.get(HttpServerExchange.class), unit.get(Sender.class), x);
        });
  }
}
