package org.jooby.internal;

import static java.util.Objects.requireNonNull;

import java.io.InputStream;

import org.jooby.Asset;
import org.jooby.MediaType;

public class InputStreamAsset implements Asset {

  private InputStream stream;

  private String name;

  private MediaType type;

  public InputStreamAsset(final InputStream stream, final String name, final MediaType type) {
    this.stream = requireNonNull(stream, "InputStream is required.");
    this.name = requireNonNull(name, "Name is required.");
    this.type = requireNonNull(type, "Type is required.");
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public String path() {
    return name;
  }

  @Override
  public long length() {
    return -1;
  }

  @Override
  public long lastModified() {
    return -1;
  }

  @Override
  public InputStream stream() throws Exception {
    return stream;
  }

  @Override
  public MediaType type() {
    return type;
  }

}
