package org.jooby.servlet;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.Part;

import org.jooby.spi.NativeUpload;

import com.google.common.collect.ImmutableList;

public class ServletUpload implements NativeUpload {

  private Part upload;

  private String tmpdir;

  private File file;

  public ServletUpload(final Part upload, final String tmpdir) {
    this.upload = requireNonNull(upload, "A part upload is required.");
    this.tmpdir = requireNonNull(tmpdir, "A tmpdir is required.");
  }

  @Override
  public void close() throws IOException {
    if (file != null) {
      file.delete();
    }
    upload.delete();
  }

  @Override
  public String name() {
    return upload.getSubmittedFileName();
  }

  @Override
  public Optional<String> header(final String name) {
    return Optional.ofNullable(upload.getHeader(name));
  }

  @Override
  public List<String> headers(final String name) {
    Collection<String> headers = upload.getHeaders(name);
    if (headers == null) {
      return Collections.emptyList();
    }
    return ImmutableList.copyOf(headers);
  }

  @Override
  public File file() throws IOException {
    if (file == null) {
      String name = "tmp-" + Long.toHexString(System.currentTimeMillis()) + "." + name();
      upload.write(name);
      file = new File(tmpdir, name);
    }
    return file;
  }

}
