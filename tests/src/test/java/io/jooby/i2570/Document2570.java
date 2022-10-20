/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i2570;

import io.jooby.FileUpload;

public class Document2570 {
  private String name;
  private FileUpload file;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public FileUpload getFile() {
    return file;
  }

  public void setFile(FileUpload file) {
    this.file = file;
  }
}
