package io.jooby.internal.jetty;

import io.jooby.Upload;
import io.jooby.Value;
import org.eclipse.jetty.http.MultiPartFormInputStream;
import org.jooby.funzy.Throwing;

import java.io.IOException;
import java.nio.file.Path;

public class JettyUpload extends Value.Simple implements Upload {
  private final MultiPartFormInputStream.MultiPart upload;

  public JettyUpload(String name, MultiPartFormInputStream.MultiPart upload) {
    super(name, upload.getSubmittedFileName());
    this.upload = upload;
  }

  @Override public String contentType() {
    return upload.getContentType();
  }

  @Override public Path path() {
    try {
      if (upload.getFile() == null) {
        upload.write("jetty" + System.currentTimeMillis() + ".tmp");
      }
      return upload.getFile().toPath();
    } catch (IOException x) {
      throw Throwing.sneakyThrow(x);
    }
  }

  @Override public long filesize() {
    return upload.getSize();
  }

  @Override public void destroy() {
    try {
      upload.cleanUp();
      upload.delete();
    } catch (IOException x) {
      throw Throwing.sneakyThrow(x);
    }
  }
}
