/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3472;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.jooby.apt.ProcessorRunner;
import io.jooby.test.MockContext;
import io.jooby.test.MockRouter;

public class Issue3472 {

  @Test
  public void shouldBindOnExternalClass() throws Exception {
    new ProcessorRunner(new C3472())
        .withRouter(
            app -> {
              var value = "xxx";
              MockRouter router = new MockRouter(app);
              MockContext ctx = new MockContext().setQueryString("?value=" + value);

              assertEquals(
                  new BindBean("mapping:" + value), router.get("/3472/mapping", ctx).value());
              assertEquals(
                  new BindBean("withName:" + value), router.get("/3472/withName", ctx).value());
            });
  }

  @Test
  public void shouldBindOnBean() throws Exception {
    new ProcessorRunner(new C3472b())
        .withRouter(
            app -> {
              var value = "zzz";
              MockRouter router = new MockRouter(app);
              MockContext ctx = new MockContext().setQueryString("?value=" + value);

              assertEquals(
                  new BindBean("bean-factory-method:" + value),
                  router.get("/3472/bean-factory-method", ctx).value());

              assertEquals(
                  "bean-constructor:" + value, router.get("/3472/bean-constructor", ctx).value());
            });
  }

  @Test
  public void shouldBindController() throws Exception {
    new ProcessorRunner(new C3472c())
        .withRouter(
            app -> {
              var value = "zzz";
              MockRouter router = new MockRouter(app);
              MockContext ctx = new MockContext().setQueryString("?value=" + value);

              assertEquals(
                  new BindController("bean-controller:" + value),
                  router.get("/3472/bean-controller", ctx).value());
            });
  }

  @Test
  public void shouldBindControllerStatic() throws Exception {
    new ProcessorRunner(new C3472d())
        .withRouter(
            app -> {
              var value = "zzz";
              MockRouter router = new MockRouter(app);
              MockContext ctx = new MockContext().setQueryString("?value=" + value);

              assertEquals(
                  new BindController("bean-controller-static:" + value),
                  router.get("/3472/bean-controller", ctx).value());
            });
  }

  @Test
  public void shouldBindWithCustomBind() throws Exception {
    new ProcessorRunner(new C3472e())
        .withRouter(
            app -> {
              var value = "yyy";
              MockRouter router = new MockRouter(app);
              MockContext ctx = new MockContext().setQueryString("?value=" + value);

              assertEquals(
                  new BindBean("mapping:" + value), router.get("/3472/custom-bind", ctx).value());
            });
  }
}
