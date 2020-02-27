package examples;

import javax.annotation.Nonnull;

public class ABean extends Bean {
  @Nonnull
  private String foo;

  public String getFoo() {
    return foo;
  }

  public void setFoo(String foo) {
    this.foo = foo;
  }
}
