/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import io.jooby.MediaType;
import io.jooby.handler.Asset;

public class OpenAPIAsset implements Asset {

  private final long lastModified;

  private final byte[] content;

  private final MediaType type;

  public OpenAPIAsset(MediaType type, byte[] content, long lastModified) {
    this.content = content;
    this.type = type;
    this.lastModified = lastModified;
  }

  @Override
  public long getSize() {
    return content.length;
  }

  @Override
  public long getLastModified() {
    return lastModified;
  }

  @Override
  public boolean isDirectory() {
    return false;
  }

  @Override
  public MediaType getContentType() {
    return type;
  }

  @Override
  public InputStream stream() {
    return new ByteArrayInputStream(content);
  }

  @Override
  public void close() throws Exception {
    // NOOP
  }
}
