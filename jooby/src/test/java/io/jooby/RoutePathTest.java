package io.jooby;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RoutePathTest {

  @Test
  public void normalize() {
    assertEquals("/", Router.normalizePath(null, false, true));
    assertEquals("/", Router.normalizePath("", false, true));
    assertEquals("/", Router.normalizePath("/", false, true));
    assertEquals("/", Router.normalizePath("//", false, true));
    assertEquals("/", Router.normalizePath("//", false, false));
    assertEquals("/", Router.normalizePath("///", false, true));
    assertEquals("/", Router.normalizePath("///", false, false));

    assertEquals("/foo", Router.normalizePath("/foo", false, true));

    assertEquals("/foo", Router.normalizePath("//foo", false, true));

    assertEquals("/foo", Router.normalizePath("foo", false, true));

    assertEquals("/foo/", Router.normalizePath("foo/", false, false));

    assertEquals("/foo/bar", Router.normalizePath("/foo/bar", false, true));

    assertEquals("/foo/bar", Router.normalizePath("/fOo/bAr", true, true));

    assertEquals("/foo/bar", Router.normalizePath("/foo/bar/", false, true));

    assertEquals("/foo/bar", Router.normalizePath("/foo///bar//", false, true));

    assertEquals("/foo/bar", Router.normalizePath("//foo/bar", false, true));

    assertEquals("/foo/bar", Router.normalizePath("/foo//bar", false, true));

    assertEquals("/foo/bar", Router.normalizePath("/foo//bar", false, true));

    assertEquals("/foo/Bar", Router.normalizePath("/foo/Bar", false, false));

    assertEquals("/foo/bar", Router.normalizePath("/foo/Bar/", true, true));

    assertEquals("/foo/bar/", Router.normalizePath("/foo/Bar/", true, false));

    assertEquals("/foo/Bar/", Router.normalizePath("/foo/Bar///", false, false));

    assertEquals("/foo/bar", Router.normalizePath("/foo/bar", false, true));
  }
}
