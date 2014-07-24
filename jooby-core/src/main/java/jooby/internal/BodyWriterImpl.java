package jooby.internal;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.function.Function;

import jooby.BodyWriter;
import jooby.HttpHeader;
import jooby.ThrowingSupplier;

class BodyWriterImpl implements BodyWriter {

  private Charset charset;

  private Function<String, HttpHeader> headers;

  private ThrowingSupplier<OutputStream> stream;

  private ThrowingSupplier<Writer> writer;

  public BodyWriterImpl(final Charset charset,
      final Function<String, HttpHeader> headers,
      final ThrowingSupplier<OutputStream> stream,
      final ThrowingSupplier<Writer> writer) {
    this.charset = requireNonNull(charset, "A charset is required.");
    this.headers = requireNonNull(headers, "A getter is required.");
    this.stream = requireNonNull(stream, "A stream is required.");
    this.writer = requireNonNull(writer, "A writer is required.");
  }

  @Override
  public Charset charset() {
    return charset;
  }

  @Override
  public HttpHeader header(final String name) {
    return headers.apply(name);
  }

  @Override
  public void text(final Text text) throws Exception {
    Writer writer = this.writer.get();
    // don't close on errors
    text.write(uncloseable(writer));
    writer.flush();
    writer.close();
  }

  @Override
  public void bytes(final Bytes bin) throws Exception {
    OutputStream out = this.stream.get();
    // don't close on errors
    bin.write(uncloseable(out));
    out.flush();
    out.close();
  }

  private static Writer uncloseable(final Writer writer) {
    return new Writer() {

      @Override
      public void write(final char[] cbuf) throws IOException {
        writer.write(cbuf);
      }

      @Override
      public void write(final int c) throws IOException {
        writer.write(c);
      }

      @Override
      public void write(final String str) throws IOException {
        writer.write(str);
      }

      @Override
      public void write(final String str, final int off, final int len) throws IOException {
        super.write(str, off, len);
      }

      @Override
      public void write(final char[] cbuf, final int off, final int len) throws IOException {
        writer.write(cbuf, off, len);
      }

      @Override
      public void flush() throws IOException {
      }

      @Override
      public void close() throws IOException {
      }
    };
  }

  private static OutputStream uncloseable(final OutputStream out) {
    return new OutputStream() {
      @Override
      public void write(final int b) throws IOException {
        out.write(b);
      }

      @Override
      public void write(final byte[] b) throws IOException {
        out.write(b);
      }

      @Override
      public void write(final byte[] b, final int off, final int len) throws IOException {
        out.write(b, off, len);
      }
    };
  }
}
