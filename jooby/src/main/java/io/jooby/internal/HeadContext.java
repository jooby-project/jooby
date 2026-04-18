/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import io.jooby.*;
import io.jooby.output.Output;

public class HeadContext extends ForwardingContext {
  /**
   * Creates a new forwarding context.
   *
   * @param context Source context.
   */
  public HeadContext(Context context) {
    super(context);
  }

  @Override
  public Context send(Path file) {
    try {
      ctx.setResponseLength(Files.size(file));
      checkSizeHeaders();
      ctx.setResponseType(MediaType.byFile(file));
      ctx.send(StatusCode.OK);
      return this;
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  @Override
  public Context send(byte[] data) {
    ctx.setResponseLength(data.length);
    checkSizeHeaders();
    ctx.send(StatusCode.OK);
    return this;
  }

  @Override
  public Context send(String data) {
    return send(data, StandardCharsets.UTF_8);
  }

  @Override
  public Context send(ByteBuffer data) {
    ctx.setResponseLength(data.remaining());
    checkSizeHeaders();
    ctx.send(StatusCode.OK);
    return this;
  }

  @Override
  public Context send(Output output) {
    ctx.setResponseLength(output.size());
    checkSizeHeaders();
    ctx.send(StatusCode.OK);
    return this;
  }

  @Override
  public Context send(FileChannel file) {
    try {
      ctx.setResponseLength(file.size());
      checkSizeHeaders();
      ctx.send(StatusCode.OK);
      return this;
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  @Override
  public Context send(FileDownload file) {
    ctx.setResponseLength(file.getFileSize());
    ctx.setResponseType(file.getContentType());
    checkSizeHeaders();
    ctx.send(StatusCode.OK);
    return this;
  }

  @Override
  public Context send(InputStream input) {
    checkSizeHeaders();
    ctx.send(StatusCode.OK);
    return this;
  }

  @Override
  public Context send(StatusCode statusCode) {
    ctx.send(statusCode);
    return this;
  }

  @Override
  public Context send(ReadableByteChannel channel) {
    checkSizeHeaders();
    ctx.send(StatusCode.OK);
    return this;
  }

  @Override
  public Context send(String data, Charset charset) {
    ctx.setResponseLength(data.getBytes(charset).length);
    checkSizeHeaders();
    ctx.send(StatusCode.OK);
    return this;
  }

  @Override
  public Context render(Object value) {
    try {
      Route route = getRoute();
      MessageEncoder encoder = route.getEncoder();
      var bytes = encoder.encode(this, value);
      if (bytes == null) {
        if (!isResponseStarted()) {
          throw new IllegalStateException("The response was not encoded");
        }
      } else {
        send(bytes);
      }
      return this;
    } catch (Exception x) {
      throw SneakyThrows.propagate(x);
    }
  }

  @Override
  public Sender responseSender() {
    checkSizeHeaders();
    ctx.send(StatusCode.OK);
    return new NoopSender();
  }

  @Override
  public OutputStream responseStream() {
    checkSizeHeaders();
    ctx.send(StatusCode.OK);
    return new NoopOutputStream();
  }

  @Override
  public PrintWriter responseWriter() {
    return new PrintWriter(responseStream());
  }

  private void checkSizeHeaders() {
    if (ctx.getResponseLength() < 0) {
      ctx.setResponseHeader("Transfer-Encoding", "chunked");
    } else {
      ctx.removeResponseHeader("Transfer-Encoding");
    }
  }

  private static class NoopOutputStream extends OutputStream {
    @Override
    public void write(byte[] b) throws IOException {}

    @Override
    public void write(byte[] b, int off, int len) throws IOException {}

    @Override
    public void write(int b) throws IOException {}
  }

  private static class NoopSender implements Sender {
    @Override
    public Sender write(byte[] data, Callback callback) {
      return this;
    }

    @Override
    public Sender write(Output output, Callback callback) {
      return this;
    }

    @Override
    public void close() {}
  }
}
