package org.jooby.internal;

import java.io.IOException;
import java.io.OutputStream;

public class OutputStreamNoClose extends OutputStream {

  private OutputStream out;

  public OutputStreamNoClose(final OutputStream out) {
    this.out = out;
  }

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

}
