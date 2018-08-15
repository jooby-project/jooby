package io.jooby.internal.utow;

import io.jooby.Value;
import io.undertow.server.handlers.form.FormData;
import io.undertow.util.Headers;
import org.jooby.funzy.Throwing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class UtowUpload extends Value.Simple implements Value.Upload {

  private final FormData.FormValue upload;

  public UtowUpload(String name, FormData.FormValue upload) {
    super(name, upload.getFileName());
    this.upload = upload;
  }

  @Override public String contentType() {
    return upload.getHeaders().getFirst(Headers.CONTENT_TYPE);
  }

  @Override public Path path() {
    return upload.getPath();
  }

  @Override public long filesize() {
    return Long.parseLong(upload.getHeaders().getFirst(Headers.CONTENT_LENGTH));
  }

  @Override public void destroy() {
    try {
      Files.deleteIfExists(upload.getPath());
    } catch (IOException x) {
      throw Throwing.sneakyThrow(x);
    }
  }
}
