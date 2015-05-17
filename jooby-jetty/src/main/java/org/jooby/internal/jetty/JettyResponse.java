package org.jooby.internal.jetty;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.HttpOutput;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.jooby.servlet.ServletServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JettyResponse extends ServletServletResponse implements Callback {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(Response.class);

  public JettyResponse(final HttpServletResponse rsp) {
    super(rsp);
  }

  @Override
  public void send(final byte[] bytes) throws Exception {
    sender().sendContent(ByteBuffer.wrap(bytes), this);
  }

  @Override
  public void send(final ByteBuffer buffer) throws Exception {
    sender().sendContent(buffer, this);
  }

  @Override
  public void send(final InputStream stream) throws Exception {
    sender().sendContent(Channels.newChannel(stream), this);
  }

  @Override
  public void send(final FileChannel channel) throws Exception {
    sender().sendContent(channel, this);
  }

  private HttpOutput sender() {
    return ((Response) rsp).getHttpOutput();
  }

  @Override
  public void succeeded() {
    // NOOP
  }

  @Override
  public void failed(final Throwable cause) {
    // TODO: will be nice to log the path of the current request
    log.error(rsp.toString(), cause);
  }

}
