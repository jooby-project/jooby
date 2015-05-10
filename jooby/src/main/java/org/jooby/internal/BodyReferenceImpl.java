package org.jooby.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;

import org.jooby.Err;
import org.jooby.Parser;
import org.jooby.Status;

import com.google.common.io.Closeables;

public class BodyReferenceImpl implements Parser.BodyReference {

  private Charset charset;

  private long length;

  private File file;

  public BodyReferenceImpl(final long length, final Charset charset, final File file,
      final InputStream in) throws IOException {
    this.length = length;
    this.charset = charset;
    if (length > 0) {
      this.file = writeTo(file, in);
    }
  }

  public BodyReferenceImpl() {
  }

  @Override
  public long length() {
    return length;
  }

  @Override
  public byte[] bytes() throws IOException {
    checkContent();
    return Files.readAllBytes(file.toPath());
  }

  @Override
  public String text() throws IOException {
    checkContent();
    return new String(bytes(), charset);
  }

  public void writeTo(final OutputStream output) throws IOException {
    Files.copy(file.toPath(), output);
  }

  private File writeTo(final File file, InputStream in) throws IOException {
    try {
      file.getParentFile().mkdirs();
      Files.copy(in, file.toPath());
    } finally {
      Closeables.closeQuietly(in);
      in = null;
    }
    return file;
  }

  private void checkContent() {
    if (file == null) {
      throw new Err(Status.BAD_REQUEST);
    }
  }

}
