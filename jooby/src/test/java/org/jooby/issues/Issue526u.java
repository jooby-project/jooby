package org.jooby.issues;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.function.Consumer;

import org.jooby.internal.RouteMatcher;
import org.jooby.internal.RoutePattern;
import org.junit.Test;

public class Issue526u {

  class RoutePathAssert {

    RoutePattern path;

    public RoutePathAssert(final String method, final String pattern) {
      path = new RoutePattern(method, pattern);
    }

    public RoutePathAssert matches(final String path) {
      return matches(path, (vars) -> {
      });
    }

    public RoutePathAssert matches(final String path, final Consumer<Map<Object, String>> vars) {
      String message = this.path + " != " + path;
      RouteMatcher matcher = this.path.matcher(path);
      boolean matches = matcher.matches();
      if (!matches) {
        System.err.println(message);
      }
      assertTrue(message, matches);
      vars.accept(matcher.vars());
      return this;
    }

    public RoutePathAssert butNot(final String path) {
      String message = this.path + " == " + path;
      RouteMatcher matcher = this.path.matcher(path);
      boolean matches = matcher.matches();
      if (matches) {
        System.err.println(message);
      }
      assertFalse(message, matches);
      return this;
    }
  }

  @Test
  public void shouldAcceptAdvancedRegexPathExpression() {
    new RoutePathAssert("GET", "/V{var:\\d{4,7}}")
        .matches("GET/V1234", (vars) -> {
          assertEquals("1234", vars.get("var"));
        })
        .matches("GET/V1234567", (vars) -> {
          assertEquals("1234567", vars.get("var"));
        })
        .butNot("GET/V123")
        .butNot("GET/V12345678");
  }

  @Test
  public void shouldAcceptSpecialChars() {
    new RoutePathAssert("GET", "/:var")
        .matches("GET/x%252Fy%252Fz", (vars) -> {
          assertEquals("x%252Fy%252Fz", vars.get("var"));
        })
        .butNot("GET/user/123/x")
        .butNot("GET/user/123x")
        .butNot("GET/user/xqi");
  }
}
