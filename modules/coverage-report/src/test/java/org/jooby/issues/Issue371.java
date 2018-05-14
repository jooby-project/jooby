package org.jooby.issues;

import static org.junit.Assert.assertTrue;

import org.jooby.test.ServerFeature;
import org.jooby.whoops.Whoops;
import org.junit.Test;

public class Issue371 extends ServerFeature {

  {
    use(new Whoops());

    get("/whoops/direct-ex", req -> {
      req.session().set("foo", "bar");

      throw new IllegalStateException("Something broken!");
    });

    get("/whoops/session-atrr", req -> {
      req.session().set("foo", "bar");

      throw new IllegalStateException("Something broken!");
    });

    get("/whoops/wrap-ex", req -> {
      try {
        return doSomething();
      } catch (Exception ex) {
        throw new IllegalStateException("Wrap wrap", ex);
      }
    });

    get("/whoops/json", req -> {
      throw new IllegalStateException("def handler");
    });
  }

  private Object doSomething() {
    throw new IllegalArgumentException("Xxx");
  }

  @Test
  public void shouldHandleDirectEx() throws Exception {
    request()
        .get("/whoops/direct-ex")
        .expect(s -> {
          assertTrue(
              s.contains(
                  "<span id=\"plain-exception\">java.lang.IllegalStateException: Something broken!"));
        });
  }

  @Test
  public void shouldDumpSessionAttr() throws Exception {
    request()
        .get("/whoops/direct-ex")
        .expect(s -> {
          assertTrue(s.contains("foo"));
          assertTrue(s.contains("bar"));
        });
  }

  @Test
  public void shouldHandleWrapEx() throws Exception {
    request()
        .get("/whoops/wrap-ex")
        .expect(s -> {
          assertTrue(
              s.contains(
                  "<span id=\"plain-exception\">java.lang.IllegalStateException: Wrap wrap"));
          assertTrue(s.contains("java.lang.IllegalArgumentException: Xxx"));
        });
  }

  @Test
  public void shouldIgnoreNonHtmlRequest() throws Exception {
    request()
        .get("/whoops/json")
        .header("Accept", "application/json")
        .expect(s -> assertTrue(s.contains("message=def handler")));
  }

}
