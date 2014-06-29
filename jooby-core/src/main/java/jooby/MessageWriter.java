package jooby;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import jooby.Response.IOSupplier;

public class MessageWriter {

  public interface Binary {
    void write(OutputStream out) throws IOException;
  }

  public interface Text {
    void write(Writer writer) throws IOException;
  }

  private IOSupplier<Writer> writer;

  private IOSupplier<OutputStream> stream;

  MessageWriter(final IOSupplier<Writer> writer, final IOSupplier<OutputStream> stream) {
    this.writer = requireNonNull(writer, "A writer is required.");
    this.stream = requireNonNull(stream, "A stream is required.");
  }

  public void text(final Text text) throws IOException {
    Writer writer = this.writer.get();
    text.write(uncloseable(writer));
    // don't close on errors
    writer.close();
  }

  public void binary(final Binary bin) throws IOException {
    OutputStream out = this.stream.get();
    bin.write(uncloseable(out));
    // don't close on errors
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
