package io.jooby;

import java.nio.file.Path;

public interface Upload extends Value {
  default String filename() {
    return value();
  }

  String contentType();

  Path path();

  long filesize();

  @Override default io.jooby.Upload upload() {
    return this;
  }

  void destroy();
}
