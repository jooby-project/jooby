/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.AttachedFile;
import io.jooby.Context;
import io.jooby.ForwardingContext;
import io.jooby.MediaType;
import io.jooby.MessageEncoder;
import io.jooby.Route;
import io.jooby.Sender;
import io.jooby.SneakyThrows;
import io.jooby.StatusCode;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
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

public class HeadContext extends ForwardingContext {
  /**
   * Creates a new forwarding context.
   *
   * @param context Source context.
   */
  public HeadContext(@Nonnull Context context) {
    super(context);
  }

  @Nonnull @Override public Context send(@Nonnull Path file) {
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

  @Nonnull @Override public Context send(@Nonnull byte[] data) {
    ctx.setResponseLength(data.length);
    checkSizeHeaders();
    ctx.send(StatusCode.OK);
    return this;
  }

  @Nonnull @Override public Context send(@Nonnull String data) {
    return send(data, StandardCharsets.UTF_8);
  }

  @Nonnull @Override public Context send(@Nonnull ByteBuffer data) {
    ctx.setResponseLength(data.remaining());
    checkSizeHeaders();
    ctx.send(StatusCode.OK);
    return this;
  }

  @Nonnull @Override public Context send(@Nonnull FileChannel file) {
    try {
      ctx.setResponseLength(file.size());
      checkSizeHeaders();
      ctx.send(StatusCode.OK);
      return this;
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  @Nonnull @Override public Context send(@Nonnull AttachedFile file) {
    ctx.setResponseLength(file.getFileSize());
    ctx.setResponseType(file.getContentType());
    checkSizeHeaders();
    ctx.send(StatusCode.OK);
    return this;
  }

  @Nonnull @Override public Context send(@Nonnull InputStream input) {
    checkSizeHeaders();
    ctx.send(StatusCode.OK);
    return this;
  }

  @Nonnull @Override public Context send(@Nonnull StatusCode statusCode) {
    ctx.send(statusCode);
    return this;
  }

  @Nonnull @Override public Context send(@Nonnull ReadableByteChannel channel) {
    checkSizeHeaders();
    ctx.send(StatusCode.OK);
    return this;
  }

  @Nonnull @Override public Context send(@Nonnull String data, @Nonnull Charset charset) {
    ctx.setResponseLength(data.getBytes(charset).length);
    checkSizeHeaders();
    ctx.send(StatusCode.OK);
    return this;
  }

  @Nonnull @Override public Context render(@Nonnull Object value) {
    try {
      Route route = getRoute();
      MessageEncoder encoder = route.getEncoder();
      byte[] bytes = encoder.encode(this, value);
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

  @Nonnull @Override public Sender responseSender() {
    checkSizeHeaders();
    ctx.send(StatusCode.OK);
    return new NoopSender();
  }

  @Nonnull @Override public OutputStream responseStream() {
    checkSizeHeaders();
    ctx.send(StatusCode.OK);
    return new NoopOutputStream();
  }

  @Nonnull @Override public PrintWriter responseWriter() {
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
    @Override public void write(@NotNull byte[] b) throws IOException {
    }

    @Override public void write(@NotNull byte[] b, int off, int len) throws IOException {
    }

    @Override public void write(int b) throws IOException {
    }
  }

  private static class NoopSender implements Sender {
    @Nonnull @Override public Sender write(@Nonnull byte[] data, @Nonnull Callback callback) {
      return this;
    }

    @Override public void close() {
    }
  }
}
