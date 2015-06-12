package org.jooby.internal.undertow;

import io.undertow.io.IoCallback;
import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;

import java.io.Closeable;
import java.io.IOException;

import org.jooby.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({HttpServerExchange.class })
public class CloseableCallbackTest {

  @Test
  public void defaults() throws Exception {
    new MockUnit(Closeable.class, IoCallback.class)
        .run(unit -> {
          new CloseableCallback(unit.get(Closeable.class), unit.get(IoCallback.class));
        });
  }

  @Test
  public void onComplete() throws Exception {
    new MockUnit(Closeable.class, IoCallback.class, HttpServerExchange.class, Sender.class)
        .expect(unit -> {
          IoCallback callback = unit.get(IoCallback.class);
          callback.onComplete(unit.get(HttpServerExchange.class), unit.get(Sender.class));
        })
        .run(unit -> {
          new CloseableCallback(unit.get(Closeable.class), unit.get(IoCallback.class))
              .onComplete(unit.get(HttpServerExchange.class), unit.get(Sender.class));
        });
  }

  @Test
  public void onException() throws Exception {
    IOException cause = new IOException();
    new MockUnit(Closeable.class, IoCallback.class, HttpServerExchange.class, Sender.class)
        .expect(unit -> {
          IoCallback callback = unit.get(IoCallback.class);
          callback.onException(unit.get(HttpServerExchange.class), unit.get(Sender.class), cause);
        })
        .run(unit -> {
          new CloseableCallback(unit.get(Closeable.class), unit.get(IoCallback.class))
              .onException(unit.get(HttpServerExchange.class), unit.get(Sender.class), cause);
        });
  }

}
