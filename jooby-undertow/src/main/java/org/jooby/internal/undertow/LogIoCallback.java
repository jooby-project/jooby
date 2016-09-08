package org.jooby.internal.undertow;

import java.io.IOException;

import org.jooby.spi.NativeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.undertow.io.IoCallback;
import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;

public class LogIoCallback implements IoCallback {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(NativeResponse.class);

  private IoCallback callback;

  public LogIoCallback(final IoCallback callback) {
    this.callback = callback;
  }

  @Override
  public void onComplete(final HttpServerExchange exchange, final Sender sender) {
    callback.onComplete(exchange, sender);
  }

  @Override
  public void onException(final HttpServerExchange exchange, final Sender sender,
      final IOException x) {
    log.error("execution of {} resulted in exception", exchange.getRequestPath(), x);
    callback.onException(exchange, sender, x);
  }

}
