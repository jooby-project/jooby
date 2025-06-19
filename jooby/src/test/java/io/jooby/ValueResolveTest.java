/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import io.jooby.internal.HashValue;

public class ValueResolveTest {

  @Test
  public void resolveOne() {
    Value value = Value.value(null, "foo", "bar");
    assertEquals("bar", value.resolve("${foo}"));
    assertEquals("- bar", value.resolve("- ${foo}"));
    assertEquals("bar-", value.resolve("${foo}-"));
    assertEquals("-", value.resolve("-"));
    assertEquals("", value.resolve(""));
  }

  @Test
  public void resolveComplexWithoutRoot() {
    HashValue value = new HashValue(null, null);
    value.put("foo.bar", "baz");
    assertEquals("baz", value.resolve("${foo.bar}"));
    assertEquals("-baz-", value.resolve("-${foo.bar}-"));
  }

  @Test
  public void resolveComplex() {
    HashValue value = new HashValue(null, null);
    value.put("firstname", "Pedro");
    value.put("lastname", "Picapiedra");
    assertEquals("Hi Pedro Picapiedra!", value.resolve("Hi ${firstname} ${lastname}!"));
  }

  @Test
  public void resolveComplexWithRoot() {
    HashValue value = new HashValue(null, "foo");
    value.put("bar", "baz");
    assertEquals("baz", value.resolve("${foo.bar}"));
    assertEquals("-baz-", value.resolve("-${foo.bar}-"));
  }

  @Test
  public void resolveMissing() {
    try {
      Value value = Value.value(null, "x", "y");
      assertEquals("bar", value.resolve("${foo}"));
    } catch (NoSuchElementException x) {
      assertEquals("Missing ${foo} at 1:1", x.getMessage());
    }

    Value value = Value.value(null, "x", "y");
    assertEquals("${foo}", value.resolve("${foo}", true));
  }
}
