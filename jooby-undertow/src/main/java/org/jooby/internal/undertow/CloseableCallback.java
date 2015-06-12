package org.jooby.internal.undertow;

import io.undertow.io.IoCallback;
import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;

import java.io.Closeable;
import java.io.IOException;

import org.xnio.IoUtils;

public class CloseableCallback implements IoCallback {

  private Closeable source;

  private IoCallback callback;

  public CloseableCallback(final Closeable source, final IoCallback callback) {
    this.source = source;
    this.callback = callback;
  }

  @Override
  public void onException(final HttpServerExchange exchange, final Sender sender,
      final IOException exception) {
    try {
      IoUtils.safeClose(source);
    } finally {
      callback.onException(exchange, sender, exception);
    }
  }

  @Override
  public void onComplete(final HttpServerExchange exchange, final Sender sender) {
    try {
      IoUtils.safeClose(source);
    } finally {
      callback.onComplete(exchange, sender);
    }
  }

}
