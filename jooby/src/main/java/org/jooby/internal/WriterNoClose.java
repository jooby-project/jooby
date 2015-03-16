package org.jooby.internal;

import java.io.IOException;
import java.io.Writer;

public class WriterNoClose extends Writer {

  private Writer writer;

  public WriterNoClose(final Writer writer) {
    this.writer= writer;
  }

  @Override
  public void write(final char[] cbuf, final int off, final int len) throws IOException {
    writer.write(cbuf, off, len);
  }

  @Override
  public void write(final String str) throws IOException {
    writer.write(str);
  }

  @Override
  public void write(final int c) throws IOException {
    writer.write(c);
  }

  @Override
  public void write(final String str, final int off, final int len) throws IOException {
    writer.write(str, off, len);
  }

  @Override
  public void write(final char[] cbuf) throws IOException {
    writer.write(cbuf);
  }

  @Override
  public void close() throws IOException {
    // NOOP
  }

  @Override
  public void flush() throws IOException {
    // NOOP
  }

}
