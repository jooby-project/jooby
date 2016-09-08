package org.jooby.internal;

import java.io.IOException;
import java.io.OutputStream;

import org.jooby.Err;
import org.jooby.Parser;
import org.jooby.Status;

public class EmptyBodyReference implements Parser.BodyReference {

  @Override
  public byte[] bytes() throws IOException {
    throw new Err(Status.BAD_REQUEST);
  }

  @Override
  public String text() throws IOException {
    throw new Err(Status.BAD_REQUEST);
  }

  @Override
  public long length() {
    return 0;
  }

  @Override
  public void writeTo(final OutputStream output) throws Exception {
    throw new Err(Status.BAD_REQUEST);
  }

}
