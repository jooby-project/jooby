package org.jooby.issues;

import javax.inject.Inject;

import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue381 extends ServerFeature {

  public static class Foo {
    private String bar;
    private String foo;

    @Inject
    public Foo(final String foo, final String bar) {
      this.foo = foo;
      this.bar = bar;
    }

    public Foo() {
    }

    public String foo() {
      return foo + ":" + bar;
    }
  }

  @Path("/381")
  public static class Controller {

    @GET
    public String withInject(final String foo, final Foo bean) {
      return foo + ":" + bean.foo();
    }

  }

  {
    bind(Foo.class);

    use(Controller.class);
  }

  @Test
  public void beanParamWithInjectAnnotation() throws Exception {
    request()
        .get("/381?foo=foo&bar=bar")
        .expect("foo:foo:bar");
  }

}
