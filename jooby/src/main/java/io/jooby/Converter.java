package io.jooby;

import javax.annotation.Nonnull;

public abstract class Converter implements Parser, Renderer {
  private String contentType;

  public Converter(@Nonnull String contentType) {
    this.contentType = contentType;
  }

  @Override public String contentType() {
    return contentType;
  }
}
