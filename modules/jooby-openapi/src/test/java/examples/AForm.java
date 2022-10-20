/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import io.jooby.FileUpload;

public class AForm extends Bean {
  private String name;

  private FileUpload picture;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public FileUpload getPicture() {
    return picture;
  }

  public void setPicture(FileUpload picture) {
    this.picture = picture;
  }
}
