package examples;

import edu.umd.cs.findbugs.annotations.NonNull;

public class ABean extends Bean {
  @NonNull
  private String foo;

  public String getFoo() {
    return foo;
  }

  public void setFoo(String foo) {
    this.foo = foo;
  }
}
