package io.jooby.i2325;

import jakarta.inject.Inject;

public class MyID2325 {

  private String value;

  @Inject
  public MyID2325(String value) {
    this.value = value;
  }

  @Override public String toString() {
    return "MyID:" + value;
  }
}
