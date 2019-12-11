package io.jooby;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RoutePathTest {

  @Test
  public void normalize() {
    assertEquals("/", Router.normalizePath(null));
    assertEquals("/", Router.normalizePath(""));
    assertEquals("/", Router.normalizePath("/"));
    assertEquals("/", Router.normalizePath("//"));
    assertEquals("/", Router.normalizePath("///"));

    assertEquals("/foo", Router.normalizePath("/foo"));

    assertEquals("/foo", Router.normalizePath("//foo"));

    assertEquals("/foo", Router.normalizePath("foo"));

    assertEquals("/foo/", Router.normalizePath("foo/"));

    assertEquals("/foo/bar", Router.normalizePath("/foo/bar"));

    assertEquals("/fOo/bAr", Router.normalizePath("/fOo/bAr"));

    assertEquals("/foo/bar/", Router.normalizePath("/foo/bar/"));

    assertEquals("/foo/bar/", Router.normalizePath("/foo///bar//"));

    assertEquals("/foo/bar", Router.normalizePath("//foo/bar"));

    assertEquals("/foo/bar", Router.normalizePath("/foo//bar"));

    assertEquals("/foo/bar", Router.normalizePath("/foo//bar"));

    assertEquals("/foo/Bar", Router.normalizePath("/foo/Bar"));

    assertEquals("/foo/Bar/", Router.normalizePath("/foo/Bar/"));

    assertEquals("/foo/Bar/", Router.normalizePath("/foo/Bar///"));

    assertEquals("/foo/bar", Router.normalizePath("/foo/bar"));
  }
}
