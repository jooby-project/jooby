/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class RoutePathTest {

  @Test
  public void normalize() {
    assertEquals("/path", Router.normalizePath("//path"));
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
