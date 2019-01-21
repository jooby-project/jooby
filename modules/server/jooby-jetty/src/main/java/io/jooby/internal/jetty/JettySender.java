package io.jooby.internal.jetty;

import io.jooby.Sender;
import org.eclipse.jetty.server.HttpOutput;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.ByteBuffer;

public class JettySender implements Sender {
  private final JettyContext ctx;
  private final HttpOutput sender;

  public JettySender(JettyContext ctx, HttpOutput sender) {
    this.ctx = ctx;
    this.sender = sender;
  }

  @Override public Sender sendBytes(@Nonnull byte[] data, @Nonnull Callback callback) {
    try {
      sender.write(data);
      sender.flush();
      callback.onComplete(ctx, null);
    } catch (IOException e) {
      callback.onComplete(ctx, e);
    }
    return this;
  }

  @Override public void close() {
    ctx.destroy(null);
  }

}
