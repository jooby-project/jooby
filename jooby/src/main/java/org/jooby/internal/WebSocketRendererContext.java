package org.jooby.internal;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jooby.MediaType;
import org.jooby.Renderer;
import org.jooby.WebSocket.ErrCallback;
import org.jooby.WebSocket.SuccessCallback;
import org.jooby.spi.NativeWebSocket;

public class WebSocketRendererContext extends AbstractRendererContext {

  private NativeWebSocket ws;

  private SuccessCallback success;

  private ErrCallback err;

  public WebSocketRendererContext(final Set<Renderer> renderers, final NativeWebSocket ws,
      final List<MediaType> produces, final Charset charset, final SuccessCallback success,
      final ErrCallback err) {
    super(renderers, produces, charset, Collections.emptyMap());
    this.ws = ws;
    this.success = success;
    this.err = err;
  }

  @Override
  protected void _send(final String text) throws Exception {
    ws.send(text, success, err);
  }

  @Override
  protected void _send(final byte[] bytes) throws Exception {
    ws.send(ByteBuffer.wrap(bytes), success, err);
  }

  @Override
  protected void _send(final ByteBuffer buffer) throws Exception {
    ws.send(buffer, success, err);
  }

  @Override
  protected void _send(final FileChannel file) throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void _send(final InputStream stream) throws Exception {
    throw new UnsupportedOperationException();
  }

}
