package source;

public class JavaBeanParam {
  private String foo;

  public String getFoo() {
    return foo;
  }

  public void setFoo(String foo) {
    this.foo = foo;
  }

  @Override public String toString() {
    return foo;
  }
}
