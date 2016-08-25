package org.jooby.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.function.Consumer;

import org.junit.Test;

public class RoutePatternTest {

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
  public void fixed() {
    new RoutePathAssert("GET", "com/test.jsp")
        .matches("GET/com/test.jsp")
        .butNot("GET/com/tsst.jsp");
  }

  @Test
  public void multipleVerb() {
    new RoutePathAssert("get|POST", "com/test.jsp")
        .matches("GET/com/test.jsp")
        .matches("POST/com/test.jsp")
        .butNot("PUT/com/test.jsp")
        .butNot("DELETE/com/test.jsp")
        .butNot("GET/com/tsst.jsp");
  }

  @Test
  public void anyVerb() {
    new RoutePathAssert("*", "com/test.jsp")
        .matches("GET/com/test.jsp")
        .matches("POST/com/test.jsp")
        .butNot("GET/com/tsst.jsp");

    new RoutePathAssert("*", "user/:id")
        .matches("GET/user/xid", (vars) -> {
          assertEquals("xid", vars.get("id"));
        })
        .matches("POST/user/xid2", (vars) -> {
          assertEquals("xid2", vars.get("id"));
        })
        .butNot("GET/com/tsst.jsp");
  }

  @Test
  public void wildOne() {
    new RoutePathAssert("GET", "com/t?st.jsp")
        .matches("GET/com/test.jsp")
        .matches("GET/com/tsst.jsp")
        .matches("GET/com/tast.jsp")
        .matches("GET/com/txst.jsp")
        .butNot("GET/com/test1.jsp");
  }

  @Test
  public void wildMany() {
    new RoutePathAssert("GET", "/profile/*/edit")
        .matches("GET/profile/ee-00-9-k/edit")
        .butNot("GET/profile/ee-00-9-k/p/edit");

    new RoutePathAssert("GET", "/profile/*/*/edit")
        .matches("GET/profile/ee-00-9-k/p/edit")
        .butNot("GET/profile/ee-00-9-k/edit")
        .butNot("GET/profile/ee-00-9-k/p/k/edit");
  }

  @Test
  public void subdir() {
    new RoutePathAssert("GET", "com/**/test.jsp")
        .matches("GET/com/test.jsp")
        .matches("GET/com/a/test.jsp")
        .butNot("GET/com/a/testx.jsp")
        .butNot("GET/org/test.jsp");

    new RoutePathAssert("GET", "com/**")
        .matches("GET/com/test.jsp")
        .matches("GET/com/a/test.jsp")
        .matches("GET/com/a/testx.jsp")
        .butNot("GET/org/test.jsp");

  }

  @Test
  public void any() {
    new RoutePathAssert("GET", "com/**")
        .matches("GET/com/test.jsp")
        .matches("GET/com/a/test.jsp")
        .matches("GET/com/a/testx.jsp")
        .butNot("GET/org/test.jsp");
  }

  @Test
  public void any2() {
    new RoutePathAssert("GET", "org/**/servlet/*.html")
        .matches("GET/org/jooby/servlet/test.html")
        .matches("GET/org/jooby/a/servlet/test.html")
        .matches("GET/org/jooby/a/b/c/servlet/test.html")
        .butNot("GET/org/jooby/a/b/c/servlet/test.js");
  }

  @Test
  public void anyNamed() {
    new RoutePathAssert("GET", "com/**:rest")
        .matches("GET/com/test.jsp", vars -> assertEquals("test.jsp", vars.get("rest")))
        .matches("GET/com/a/test.jsp", vars -> assertEquals("a/test.jsp", vars.get("rest")))
        .matches("GET/com/", vars -> assertEquals("", vars.get("rest")))
        .butNot("GET/com")
        .butNot("GET/test.jsp");
  }

  @Test
  public void anyNamedSpring() {
    new RoutePathAssert("GET", "com/{rest:**}")
        .matches("GET/com/test.jsp", vars -> assertEquals("test.jsp", vars.get("rest")))
        .matches("GET/com/a/test.jsp", vars -> assertEquals("a/test.jsp", vars.get("rest")))
        .matches("GET/com/", vars -> assertEquals("", vars.get("rest")))
        .butNot("GET/com")
        .butNot("GET/test.jsp");
  }

  @Test
  public void anyNamedInner() {
    new RoutePathAssert("GET", "com/**:rest/bar")
        .matches("GET/com/foo/bar", (vars) -> assertEquals("foo", vars.get("rest")))
        .matches("GET/com/a/foo/bar", (vars) -> assertEquals("a/foo", vars.get("rest")))
        .butNot("GET/com/foo/baz")
        .butNot("GET/test.jsp");
  }

  @Test
  public void anyNamedInnerSpring() {
    new RoutePathAssert("GET", "com/{rest:**}/bar")
        .matches("GET/com/foo/bar", (vars) -> assertEquals("foo", vars.get("rest")))
        .matches("GET/com/a/foo/bar", (vars) -> assertEquals("a/foo", vars.get("rest")))
        .butNot("GET/com/foo/baz")
        .butNot("GET/test.jsp");
  }

  @Test
  public void anyNamedMulti() {
    new RoutePathAssert("GET", "com/**:first/bar/**:second")
        .matches("GET/com/foo/bar/moo", (vars) -> {
          assertEquals("foo", vars.get("first"));
          assertEquals("moo", vars.get("second"));
        })
        .matches("GET/com/a/foo/bar/moo/baz", (vars) -> {
          assertEquals("a/foo", vars.get("first"));
          assertEquals("moo/baz", vars.get("second"));
        })
        .butNot("GET/com/foo/baz")
        .butNot("GET/test.jsp");
  }

  @Test
  public void anyNamedMultiSpring() {
    new RoutePathAssert("GET", "com/{first:**}/bar/{second:**}")
        .matches("GET/com/foo/bar/moo", (vars) -> {
          assertEquals("foo", vars.get("first"));
          assertEquals("moo", vars.get("second"));
        })
        .matches("GET/com/a/foo/bar/moo/baz", (vars) -> {
          assertEquals("a/foo", vars.get("first"));
          assertEquals("moo/baz", vars.get("second"));
        })
        .butNot("GET/com/foo/baz")
        .butNot("GET/test.jsp");
  }

  @Test
  public void anyNamedMultiMixed() {
    new RoutePathAssert("GET", "com/**:first/bar/{second:**}")
        .matches("GET/com/foo/bar/moo", (vars) -> {
          assertEquals("foo", vars.get("first"));
          assertEquals("moo", vars.get("second"));
        })
        .matches("GET/com/a/foo/bar/moo/baz", (vars) -> {
          assertEquals("a/foo", vars.get("first"));
          assertEquals("moo/baz", vars.get("second"));
        })
        .butNot("GET/com/foo/baz")
        .butNot("GET/test.jsp");
  }

  @Test
  public void anyNamedMultiMixed2() {
    new RoutePathAssert("GET", "com/{first:**}/bar/**:second")
        .matches("GET/com/foo/bar/moo", (vars) -> {
          assertEquals("foo", vars.get("first"));
          assertEquals("moo", vars.get("second"));
        })
        .matches("GET/com/a/foo/bar/moo/baz", (vars) -> {
          assertEquals("a/foo", vars.get("first"));
          assertEquals("moo/baz", vars.get("second"));
        })
        .butNot("GET/com/foo/baz")
        .butNot("GET/test.jsp");
  }

  @Test
  public void rootVar() {
    new RoutePathAssert("GET", "{id}/list")
        .matches("GET/xqi/list")
        .matches("GET/123/list")
        .butNot("GET/123/lisx");
  }

  @Test
  public void mixedVar() {
    new RoutePathAssert("GET", "user/:id/:name")
        .matches("GET/user/xqi/n", (vars) -> {
          assertEquals("xqi", vars.get("id"));
          assertEquals("n", vars.get("name"));
        })
        .butNot("GET/user/123/x/y");

    new RoutePathAssert("GET", "user/{id}/{name}")
        .matches("GET/user/xqi/n", (vars) -> {
          assertEquals("xqi", vars.get("id"));
          assertEquals("n", vars.get("name"));
        })
        .butNot("GET/user/123/x/y");

    new RoutePathAssert("GET", "user/{id}/:name")
        .matches("GET/user/xqi/n", (vars) -> {
          assertEquals("xqi", vars.get("id"));
          assertEquals("n", vars.get("name"));
        })
        .butNot("GET/user/123/x/y");

    new RoutePathAssert("GET", "user/:id/{name}")
        .matches("GET/user/xqi/n", (vars) -> {
          assertEquals("xqi", vars.get("id"));
          assertEquals("n", vars.get("name"));
        })
        .butNot("GET/user/123/x/y");
  }

  @Test
  public void var() {
    new RoutePathAssert("GET", "user/{id}")
        .matches("GET/user/xqi", (vars) -> {
          assertEquals("xqi", vars.get("id"));
        })
        .matches("GET/user/123", (vars) -> {
          assertEquals("123", vars.get("id"));
        })
        .butNot("GET/user/123/x");

    new RoutePathAssert("GET", "user/:id")
        .matches("GET/user/xqi", (vars) -> {
          assertEquals("xqi", vars.get("id"));
        })
        .matches("GET/user/123", (vars) -> {
          assertEquals("123", vars.get("id"));
        })
        .butNot("GET/user/123/x");

    new RoutePathAssert("GET", "/:id")
        .matches("GET/xqi", (vars) -> {
          assertEquals("xqi", vars.get("id"));
        })
        .matches("GET/123", (vars) -> {
          assertEquals("123", vars.get("id"));
        })
        .butNot("GET/");

    new RoutePathAssert("GET", "/{id}")
        .matches("GET/xqi", (vars) -> {
          assertEquals("xqi", vars.get("id"));
        })
        .matches("GET/123", (vars) -> {
          assertEquals("123", vars.get("id"));
        })
        .butNot("GET/");
  }

  @Test
  public void varWithPrefix() {
    new RoutePathAssert("GET", "user/p{id}")
        .matches("GET/user/pxqi", (vars) -> {
          assertEquals("xqi", vars.get("id"));
        })
        .matches("GET/user/p123", (vars) -> {
          assertEquals("123", vars.get("id"));
        })
        .butNot("GET/user/p123/x");

    new RoutePathAssert("GET", "user/p:id")
        .matches("GET/user/pxqi", (vars) -> {
          assertEquals("xqi", vars.get("id"));
        })
        .matches("GET/user/p123", (vars) -> {
          assertEquals("123", vars.get("id"));
        })
        .butNot("GET/user/p123/x");
  }

  @Test
  public void regex() {
    new RoutePathAssert("GET", "user/{id:\\d+}")
        .matches("GET/user/123", (vars) -> {
          assertEquals("123", vars.get("id"));
        })
        .butNot("GET/user/123/x")
        .butNot("GET/user/123x")
        .butNot("GET/user/xqi");
  }

  @Test
  public void antExamples() {
    new RoutePathAssert("GET", "*.java")
        .matches("GET/.java")
        .matches("GET/x.java")
        .matches("GET/FooBar.java")
        .butNot("GET/FooBar.xml");

    new RoutePathAssert("GET", "?.java")
        .matches("GET/x.java")
        .matches("GET/A.java")
        .butNot("GET/.java")
        .butNot("GET/xyz.java");

    new RoutePathAssert("GET", "**/CVS/*")
        .matches("GET/CVS/Repository")
        .matches("GET/org/apache/CVS/Entries")
        .matches("GET/org/apache/jakarta/tools/ant/CVS/Entries")
        .butNot("GET/org/apache/CVS/foo/bar/Entries");

    new RoutePathAssert("GET", "org/apache/jakarta/**")
        .matches("GET/org/apache/jakarta/tools/ant/docs/index.html")
        .matches("GET/org/apache/jakarta/test.xml")
        .butNot("GET/org/apache/xyz.java");

    new RoutePathAssert("GET", "org/apache/**/CVS/*")
        .matches("GET/org/apache/CVS/Entries")
        .matches("GET/org/apache/jakarta/tools/ant/CVS/Entries")
        .butNot("GET/org/apache/CVS/foo/bar/Entries");
  }

  @Test
  public void moreExpression() {
    new RoutePathAssert("GET", "/views/products/**/*.cfm")
        .matches("GET/views/products/index.cfm")
        .matches("GET/views/products/SE10/index.cfm")
        .matches("GET/views/products/SE10/details.cfm")
        .matches("GET/views/products/ST80/index.cfm")
        .matches("GET/views/products/ST80/details.cfm")
        .butNot("GET/views/index.cfm")
        .butNot("GET/views/aboutUs/index.cfm")
        .butNot("GET/views/aboutUs/managementTeam.cfm");

    new RoutePathAssert("GET", "/views/index??.cfm")
        .matches("GET/views/index01.cfm")
        .matches("GET/views/index02.cfm")
        .matches("GET/views/indexAA.cfm")
        .butNot("GET/views/index01.htm")
        .butNot("GET/views/index1.cfm")
        .butNot("GET/views/indexOther.cfm")
        .butNot("GET/views/anotherDir/index01.cfm");
  }

  @Test
  public void normalizePath() {
    assertEquals("/", new RoutePattern("GET", "/").pattern());
    assertEquals("/", new RoutePattern("GET", "//").pattern());
    assertEquals("/foo", new RoutePattern("GET", "/foo//").pattern());
    assertEquals("/foo", new RoutePattern("GET", "foo//").pattern());
    assertEquals("/foo", new RoutePattern("GET", "foo").pattern());
    assertEquals("/foo", new RoutePattern("GET", "foo/").pattern());
    assertEquals("/foo/bar", new RoutePattern("GET", "/foo//bar").pattern());
  }

  @Test
  public void capturingGroups() {
    new RoutePathAssert("GET", "/js/*/2.1.3/*")
        .matches("GET/js/jquery/2.1.3/jquery.js", vars -> {
          assertEquals("jquery", vars.get(0));
          assertEquals("jquery.js", vars.get(1));
        });

    new RoutePathAssert("GET", "/js/**")
        .matches("GET/js/jquery/2.1.3/jquery.js", vars -> {
          assertEquals("jquery/2.1.3/jquery.js", vars.get(0));
        });

    new RoutePathAssert("GET", "/js/**/*.js")
        .matches("GET/js/jquery/2.1.3/jquery.js", vars -> {
          assertEquals("jquery/2.1.3/jquery", vars.get(0));
        });
  }

  @Test
  public void cornerCase() {
    new RoutePathAssert("GET", "/search/**")
        .matches("GET/search");

    new RoutePathAssert("GET", "/m/**")
        .butNot("GET/merge/login");

  }

}
