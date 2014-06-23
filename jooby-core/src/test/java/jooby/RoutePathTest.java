package jooby;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RoutePathTest {

  @Test
  public void staticPath() {
    assertTrue(path("GET", "/hello").matches("GET/hello"));
  }

  @Test
  public void zeroOrMoreChars() {
    assertTrue(path("GET", "/*.java").matches("GET/.java"));
    assertTrue(path("GET", "/*.java").matches("GET/x.java"));
    assertTrue(path("GET", "/*.java").matches("GET/FooBar.java"));
    assertFalse(path("GET", "/*.java").matches("GET/FooBar.xml"));
  }

  @Test
  public void oneChar() {
    assertTrue(path("GET", "/?.java").matches("GET/x.java"));
    assertTrue(path("GET", "/?.java").matches("GET/A.java"));
    assertFalse(path("GET", "/?.java").matches("GET/.java"));
    assertFalse(path("GET", "/?.java").matches("GET/xyz.java"));
  }

  @Test
  public void mix() {
    assertTrue(path("GET", "/?abc/*/*.java").matches("GET/xabc/foobar/test.java"));
  }

  @Test
  public void recursive() {
    assertTrue(path("GET", "/test/**").matches("GET/test/x.java"));
    assertFalse(path("GET", "/test/**").matches("GET/xyz.html"));

    assertTrue(path("GET", "/**/CVS/*").matches("GET/CVS/Repository"));
    assertFalse(path("GET", "/**/CVS/*").matches("GET/org/apache/CVS/foo/bar/Entries"));

    assertTrue(path("GET", "/org/apache/jakarta/**").matches(
        "GET/org/apache/jakarta/tools/ant/docs/index.html"));
    assertTrue(path("GET", "/org/apache/jakarta/**").matches(
        "GET/org/apache/jakarta/test.xml"));
    assertFalse(path("GET", "/org/apache/jakarta/**").matches(
        "GET/org/apache/xyz.java"));

    assertTrue(path("GET", "/**/*.js").matches("GET/f1.js"));
    assertTrue(path("GET", "/**/*.js").matches("GET/js/f1.js"));
    assertTrue(path("GET", "/**/*.js").matches("GET/js/lib/jquery.js"));
  }

  public RoutePath path(final String method, final String pattern) {
    RoutePath routePath = new RoutePath(method, pattern);
    System.out.println(routePath);
    return routePath;
  }
}
