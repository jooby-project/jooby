package examples;

import io.jooby.annotations.Path;
import io.jooby.annotations.QueryParam;

public class NullInjection {

  public static class QParam {
    int foo;

    Integer bar;

    public QParam(int foo, Integer bar) {
      this.foo = foo;
      this.bar = bar;
    }

    public void setBaz(int baz) {

    }

    @Override public String toString() {
      return foo + ":" + bar;
    }
  }

  @Path("/nonnull")
  public Object nonnulArg(@QueryParam int v) {
    return v;
  }

  @Path("/nullok")
  public Object nonnulArg(@QueryParam Integer v) {
    return String.valueOf(v);
  }

  @Path("/nullbean")
  public Object nonnulArg(@QueryParam QParam bean) {
    return bean;
  }
}
