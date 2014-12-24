package org.jooby.internal.undertow;

import io.undertow.io.BlockingSenderImpl;
import io.undertow.io.Sender;
import io.undertow.io.UndertowInputStream;
import io.undertow.server.BlockingHttpExchange;
import io.undertow.server.HttpServerExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class TmpBlockingHttpExchange implements BlockingHttpExchange {

  private InputStream inputStream;
  private OutputStream outputStream;
  private Sender sender;
  private final HttpServerExchange exchange;

  TmpBlockingHttpExchange(final HttpServerExchange exchange) {
    this.exchange = exchange;
  }

  @Override
  public InputStream getInputStream() {
    if (inputStream == null) {
      inputStream = new UndertowInputStream(exchange);
    }
    return inputStream;
  }

  @Override
  public OutputStream getOutputStream() {
    if (outputStream == null) {
      outputStream = new TmpOutputStream(exchange);
    }
    return outputStream;
  }

  @Override
  public Sender getSender() {
    if (sender == null) {
      sender = new BlockingSenderImpl(exchange, getOutputStream());
    }
    return sender;
  }

  @Override
  public void close() throws IOException {
    try {
      getInputStream().close();
    } finally {
      getOutputStream().close();
    }
  }

}
