package org.jooby.internal.jetty;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.HttpOutput;
import org.eclipse.jetty.server.Response;
import org.jooby.servlet.ServletServletResponse;

public class JettyResponse extends ServletServletResponse {

  public JettyResponse(final HttpServletResponse rsp) {
    super(rsp);
  }

  @Override
  public void send(final byte[] bytes) throws Exception {
    HttpOutput output = output();
    output.sendContent(ByteBuffer.wrap(bytes));
    output.close();
  }

  @Override
  public void send(final ByteBuffer buffer) throws Exception {
    HttpOutput output = output();
    output.sendContent(buffer);
    output.close();
  }

  @Override
  public void send(final InputStream stream) throws Exception {
    HttpOutput output = output();
    output.sendContent(stream);
    stream.close();
    output.close();
  }

  @Override
  public void send(final FileChannel channel) throws Exception {
    HttpOutput output = output();
    output.sendContent(channel);
    output.close();
  }

  private HttpOutput output() {
    return ((Response) rsp).getHttpOutput();
  }

}
