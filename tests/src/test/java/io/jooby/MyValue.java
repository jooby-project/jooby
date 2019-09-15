package io.jooby;

public class MyValue {
  private String string;

  public String getString() {
    return string;
  }

  public void setString(String string) {
    this.string = string;
  }

  @Override public String toString() {
    return string;
  }
}
