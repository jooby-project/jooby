package tests.i2325;

import java.util.Optional;

public class MyID2325 {

  private String value;

  public MyID2325(String value) {
    this.value = value;
  }

  @Override public String toString() {
    return "MyID:" + Optional.ofNullable(value).orElse("{}");
  }
}
